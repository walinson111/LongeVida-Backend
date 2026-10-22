package com.longevida.api.service;

import com.longevida.api.dto.ProgressoDto;
import com.longevida.api.entity.Atividade;
import com.longevida.api.entity.ConfiguracaoUsuario;
import com.longevida.api.entity.Refeicao;
import com.longevida.api.entity.RegistroAgua;
import com.longevida.api.entity.RegistroPeso;
import com.longevida.api.entity.Usuario;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.AtividadeRepository;
import com.longevida.api.repository.ConfiguracaoUsuarioRepository;
import com.longevida.api.repository.RefeicaoRepository;
import com.longevida.api.repository.RegistroAguaRepository;
import com.longevida.api.repository.RegistroPesoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumo diário, histórico, gráficos de progresso, peso e água. */
@Service
public class ProgressoService {

    private static final int MAX_DIAS_PROGRESSO = 90;
    private static final int MAX_DIAS_HISTORICO = 366;
    private static final int MAX_ITENS_HISTORICO = 200;

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final RefeicaoRepository refeicaoRepository;
    private final AtividadeRepository atividadeRepository;
    private final RegistroPesoRepository pesoRepository;
    private final RegistroAguaRepository aguaRepository;
    private final Clock clock;

    public ProgressoService(UsuarioRepository usuarioRepository,
                            ConfiguracaoUsuarioRepository configuracaoRepository,
                            RefeicaoRepository refeicaoRepository,
                            AtividadeRepository atividadeRepository,
                            RegistroPesoRepository pesoRepository,
                            RegistroAguaRepository aguaRepository,
                            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.refeicaoRepository = refeicaoRepository;
        this.atividadeRepository = atividadeRepository;
        this.pesoRepository = pesoRepository;
        this.aguaRepository = aguaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProgressoDto.ResumoDiario resumoDiario(Long usuarioId, LocalDate data) {
        LocalDate dia = data != null ? data : LocalDate.now(clock);
        ConfiguracaoUsuario cfg = configuracaoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configurações não encontradas."));

        RefeicaoRepository.TotalDia comida = refeicaoRepository.totaisPorDia(usuarioId, dia, dia)
                .stream().findFirst().orElse(null);
        AtividadeRepository.TotalDia exercicio = atividadeRepository.totaisPorDia(usuarioId, dia, dia)
                .stream().findFirst().orElse(null);

        return new ProgressoDto.ResumoDiario(
                dia,
                comida != null ? comida.getCalorias() : 0L,
                comida != null ? comida.getProteinas() : BigDecimal.ZERO,
                comida != null ? comida.getCarboidratos() : BigDecimal.ZERO,
                comida != null ? comida.getGorduras() : BigDecimal.ZERO,
                exercicio != null ? exercicio.getCalorias() : 0L,
                exercicio != null ? exercicio.getMinutos() : 0L,
                aguaRepository.totalDoDia(usuarioId, dia),
                new ProgressoDto.Metas(cfg.getMetaCalorias(), cfg.getMetaAguaMl(), cfg.getMetaExercicioMin(),
                        cfg.getMetaCaloriasGastas()));
    }

    @Transactional(readOnly = true)
    public List<ProgressoDto.ItemHistorico> historico(Long usuarioId, ProgressoDto.TipoHistorico tipo,
                                                      LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now(clock);
        LocalDate inicio = de != null ? de : fim.minusDays(29);
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial não pode ser depois da data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAX_DIAS_HISTORICO) {
            throw new RegraNegocioException("O período máximo é de " + MAX_DIAS_HISTORICO + " dias.");
        }

        List<ProgressoDto.ItemHistorico> itens = new ArrayList<>();
        if (tipo == null || tipo == ProgressoDto.TipoHistorico.REFEICAO) {
            for (Refeicao r : refeicaoRepository
                    .findByUsuarioIdAndDataRefeicaoBetweenOrderByDataRefeicaoDescHorarioDesc(usuarioId, inicio, fim)) {
                int kcal = r.getItens().stream().mapToInt(i -> i.getCalorias()).sum();
                itens.add(new ProgressoDto.ItemHistorico("REFEICAO", r.getId(), r.getNome(), r.getDataRefeicao(),
                        r.getHorario(), kcal, null));
            }
        }
        if (tipo == null || tipo == ProgressoDto.TipoHistorico.ATIVIDADE) {
            for (Atividade a : atividadeRepository
                    .findByUsuarioIdAndDataAtividadeBetweenOrderByDataAtividadeDescHorarioDesc(usuarioId, inicio, fim)) {
                itens.add(new ProgressoDto.ItemHistorico("ATIVIDADE", a.getId(), a.getNome(), a.getDataAtividade(),
                        a.getHorario(), a.getCaloriasGastas(), a.getDuracaoMin()));
            }
        }
        return itens.stream()
                .sorted(Comparator.comparing(ProgressoDto.ItemHistorico::data)
                        .thenComparing(ProgressoDto.ItemHistorico::horario)
                        .reversed())
                .limit(MAX_ITENS_HISTORICO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressoDto.Progresso progresso(Long usuarioId, int dias) {
        if (dias < 2 || dias > MAX_DIAS_PROGRESSO) {
            throw new RegraNegocioException("Informe um período entre 2 e " + MAX_DIAS_PROGRESSO + " dias.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusDays(dias - 1L);

        // Calorias: um ponto por dia do período (dias sem registro valem 0).
        Map<LocalDate, Long> consumidas = new HashMap<>();
        refeicaoRepository.totaisPorDia(usuarioId, inicio, fim).forEach(t -> consumidas.put(t.getData(), t.getCalorias()));
        Map<LocalDate, Long> gastas = new HashMap<>();
        atividadeRepository.totaisPorDia(usuarioId, inicio, fim).forEach(t -> gastas.put(t.getData(), t.getCalorias()));

        List<ProgressoDto.PontoCalorias> calorias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            calorias.add(new ProgressoDto.PontoCalorias(d, consumidas.getOrDefault(d, 0L), gastas.getOrDefault(d, 0L)));
        }

        // Peso: só os dias em que o usuário registrou.
        List<ProgressoDto.PontoPeso> pesos = pesoRepository
                .findByUsuarioIdAndDataBetweenOrderByDataAsc(usuarioId, inicio, fim).stream()
                .map(r -> new ProgressoDto.PontoPeso(r.getData(), r.getPesoKg()))
                .toList();
        BigDecimal variacao = pesos.size() >= 2
                ? pesos.get(pesos.size() - 1).peso().subtract(pesos.get(0).peso())
                : null;
        BigDecimal pesoAtual = pesoRepository.findFirstByUsuarioIdOrderByDataDesc(usuarioId)
                .map(RegistroPeso::getPesoKg)
                .orElse(usuario.getPesoKg());

        return new ProgressoDto.Progresso(dias, pesoAtual, variacao, pesos, calorias,
                new ProgressoDto.ResumoCorporal(usuario.getAlturaCm(), pesoAtual, calcularImc(pesoAtual, usuario.getAlturaCm()),
                        usuario.getNivelAtividade()));
    }

    /** Grava (ou substitui) o peso do dia e mantém o "peso atual" do usuário em dia. */
    @Transactional
    public ProgressoDto.PontoPeso registrarPeso(Long usuarioId, BigDecimal peso, LocalDate data) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        LocalDate hoje = LocalDate.now(clock);
        LocalDate dia = data != null ? data : hoje;
        if (dia.isAfter(hoje)) {
            throw new RegraNegocioException("A data do peso não pode ser futura.");
        }

        RegistroPeso registro = pesoRepository.findByUsuarioIdAndData(usuarioId, dia).orElseGet(() -> {
            RegistroPeso novo = new RegistroPeso();
            novo.setUsuario(usuario);
            novo.setData(dia);
            return novo;
        });
        registro.setPesoKg(peso.setScale(1, RoundingMode.HALF_UP));
        pesoRepository.saveAndFlush(registro);

        pesoRepository.findFirstByUsuarioIdOrderByDataDesc(usuarioId).ifPresent(maisRecente -> {
            usuario.setPesoKg(maisRecente.getPesoKg());
            usuarioRepository.save(usuario);
        });
        return new ProgressoDto.PontoPeso(registro.getData(), registro.getPesoKg());
    }

    @Transactional
    public ProgressoDto.AguaResposta registrarAgua(Long usuarioId, int quantidadeMl) {
        ConfiguracaoUsuario cfg = configuracaoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configurações não encontradas."));
        LocalDate hoje = LocalDate.now(clock);

        RegistroAgua registro = new RegistroAgua();
        registro.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        registro.setData(hoje);
        registro.setQuantidadeMl(quantidadeMl);
        aguaRepository.saveAndFlush(registro);

        return new ProgressoDto.AguaResposta(aguaRepository.totalDoDia(usuarioId, hoje), cfg.getMetaAguaMl());
    }

    private static BigDecimal calcularImc(BigDecimal peso, Integer alturaCm) {
        if (peso == null || alturaCm == null || alturaCm <= 0) {
            return null;
        }
        BigDecimal alturaM = BigDecimal.valueOf(alturaCm).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return peso.divide(alturaM.multiply(alturaM), 1, RoundingMode.HALF_UP);
    }
}
