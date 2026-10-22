package com.longevida.api.service;

import com.longevida.api.dto.RefeicaoDto;
import com.longevida.api.entity.Alimento;
import com.longevida.api.entity.Refeicao;
import com.longevida.api.entity.RefeicaoItem;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.AlimentoRepository;
import com.longevida.api.repository.RefeicaoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefeicaoService {

    private static final int MAX_DIAS = 366;

    private final RefeicaoRepository refeicaoRepository;
    private final AlimentoRepository alimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public RefeicaoService(RefeicaoRepository refeicaoRepository,
                           AlimentoRepository alimentoRepository,
                           UsuarioRepository usuarioRepository,
                           Clock clock) {
        this.refeicaoRepository = refeicaoRepository;
        this.alimentoRepository = alimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public RefeicaoDto.Resposta registrar(Long usuarioId, RefeicaoDto.Request req) {
        Set<Long> ids = req.itens().stream().map(RefeicaoDto.ItemRequest::alimentoId).collect(Collectors.toSet());
        Map<Long, Alimento> alimentos = alimentoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Alimento::getId, a -> a));

        Refeicao refeicao = new Refeicao();
        refeicao.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        refeicao.setNome(req.nome().trim());
        refeicao.setDataRefeicao(req.data() != null ? req.data() : LocalDate.now(clock));
        refeicao.setHorario(req.horario().truncatedTo(ChronoUnit.MINUTES));

        for (RefeicaoDto.ItemRequest itemReq : req.itens()) {
            Alimento alimento = alimentos.get(itemReq.alimentoId());
            if (alimento == null) {
                throw new RegraNegocioException("Alimento não encontrado no catálogo.");
            }
            refeicao.adicionarItem(calcularItem(alimento, itemReq.quantidade()));
        }
        return paraDto(refeicaoRepository.save(refeicao));
    }

    @Transactional(readOnly = true)
    public List<RefeicaoDto.Resposta> listar(Long usuarioId, LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now(clock);
        LocalDate inicio = de != null ? de : fim;
        if (inicio.isAfter(fim) || ChronoUnit.DAYS.between(inicio, fim) > MAX_DIAS) {
            throw new RegraNegocioException("Período inválido (máximo de " + MAX_DIAS + " dias).");
        }
        return refeicaoRepository
                .findByUsuarioIdAndDataRefeicaoBetweenOrderByDataRefeicaoDescHorarioDesc(usuarioId, inicio, fim)
                .stream().map(RefeicaoService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public RefeicaoDto.Resposta obter(Long usuarioId, Long id) {
        return paraDto(buscar(usuarioId, id));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        refeicaoRepository.delete(buscar(usuarioId, id));
    }

    private Refeicao buscar(Long usuarioId, Long id) {
        return refeicaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Refeição não encontrada."));
    }

    /**
     * Proporção = quantidade informada / porção de referência do catálogo.
     * Ex.: 150 g de arroz (porção 100 g) => fator 1,5. Funciona para g, ml e unidade.
     */
    static RefeicaoItem calcularItem(Alimento a, BigDecimal quantidade) {
        BigDecimal fator = quantidade.divide(a.getPorcaoQuantidade(), 10, RoundingMode.HALF_UP);
        RefeicaoItem item = new RefeicaoItem();
        item.setAlimento(a);
        item.setNomeAlimento(a.getNome());
        item.setQuantidade(quantidade.setScale(2, RoundingMode.HALF_UP));
        item.setUnidade(a.getPorcaoUnidade());
        item.setCalorias(a.getCalorias().multiply(fator).setScale(0, RoundingMode.HALF_UP).intValue());
        item.setProteinas(a.getProteinas().multiply(fator).setScale(1, RoundingMode.HALF_UP));
        item.setCarboidratos(a.getCarboidratos().multiply(fator).setScale(1, RoundingMode.HALF_UP));
        item.setGorduras(a.getGorduras().multiply(fator).setScale(1, RoundingMode.HALF_UP));
        return item;
    }

    private static RefeicaoDto.Resposta paraDto(Refeicao r) {
        List<RefeicaoDto.ItemResposta> itens = r.getItens().stream()
                .map(i -> new RefeicaoDto.ItemResposta(i.getId(),
                        i.getAlimento() != null ? i.getAlimento().getId() : null,
                        i.getNomeAlimento(), i.getQuantidade(), i.getUnidade(), i.getCalorias(),
                        i.getProteinas(), i.getCarboidratos(), i.getGorduras()))
                .toList();
        int calorias = itens.stream().mapToInt(RefeicaoDto.ItemResposta::calorias).sum();
        BigDecimal proteinas = soma(itens, RefeicaoDto.ItemResposta::proteinas);
        BigDecimal carboidratos = soma(itens, RefeicaoDto.ItemResposta::carboidratos);
        BigDecimal gorduras = soma(itens, RefeicaoDto.ItemResposta::gorduras);
        return new RefeicaoDto.Resposta(r.getId(), r.getNome(), r.getDataRefeicao(), r.getHorario(), itens,
                new RefeicaoDto.Totais(calorias, proteinas, carboidratos, gorduras));
    }

    private static BigDecimal soma(List<RefeicaoDto.ItemResposta> itens,
                                   java.util.function.Function<RefeicaoDto.ItemResposta, BigDecimal> campo) {
        return itens.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(1, RoundingMode.HALF_UP);
    }
}
