package com.longevida.api.service;

import com.longevida.api.dto.DispositivoDto;
import com.longevida.api.entity.Dispositivo;
import com.longevida.api.entity.MedicaoWearable;
import com.longevida.api.entity.TipoMedicao;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.DispositivoRepository;
import com.longevida.api.repository.MedicaoWearableRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estrutura para wearables: cadastro de dispositivos e recepção de medições.
 * A leitura do relógio (HealthKit, Health Connect, API do fabricante) acontece fora do
 * backend e chega aqui pelo endpoint de medições. Ver README.
 */
@Service
public class DispositivoService {

    private static final int LIMITE_DISPOSITIVOS = 10;

    private final DispositivoRepository dispositivoRepository;
    private final MedicaoWearableRepository medicaoRepository;
    private final UsuarioRepository usuarioRepository;

    public DispositivoService(DispositivoRepository dispositivoRepository,
                              MedicaoWearableRepository medicaoRepository,
                              UsuarioRepository usuarioRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.medicaoRepository = medicaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DispositivoDto.Resposta> listar(Long usuarioId) {
        return dispositivoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(DispositivoService::paraDto).toList();
    }

    @Transactional
    public DispositivoDto.Resposta cadastrar(Long usuarioId, DispositivoDto.Request req) {
        if (dispositivoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).size() >= LIMITE_DISPOSITIVOS) {
            throw new RegraNegocioException("Limite de " + LIMITE_DISPOSITIVOS + " dispositivos atingido.");
        }
        Dispositivo d = new Dispositivo();
        d.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        d.setNome(req.nome().trim());
        d.setMarca(req.marca().trim());
        d.setIdentificadorExterno(req.identificadorExterno() == null || req.identificadorExterno().isBlank()
                ? null : req.identificadorExterno().trim());
        d.setConectado(true);
        return paraDto(dispositivoRepository.save(d));
    }

    @Transactional
    public DispositivoDto.Resposta alterarConexao(Long usuarioId, Long id, boolean conectado) {
        Dispositivo d = buscar(usuarioId, id);
        d.setConectado(conectado);
        return paraDto(dispositivoRepository.save(d));
    }

    @Transactional
    public void remover(Long usuarioId, Long id) {
        dispositivoRepository.delete(buscar(usuarioId, id));
    }

    /** Recebe um lote de medições. Medições repetidas (mesmo tipo e instante) são ignoradas. */
    @Transactional
    public DispositivoDto.IngestaoResposta registrarMedicoes(Long usuarioId, Long dispositivoId,
                                                             DispositivoDto.MedicoesRequest req) {
        Dispositivo dispositivo = buscar(usuarioId, dispositivoId);
        if (!dispositivo.isConectado()) {
            throw new RegraNegocioException("O dispositivo está desconectado.");
        }
        Instant limiteFuturo = Instant.now().plus(5, ChronoUnit.MINUTES);

        List<MedicaoWearable> novas = new ArrayList<>();
        for (DispositivoDto.MedicaoRequest m : req.medicoes()) {
            if (m.valor().compareTo(m.tipo().getMaximo()) > 0) {
                throw new RegraNegocioException("Valor fora do limite para " + m.tipo().name() + ".");
            }
            if (m.medidoEm().isAfter(limiteFuturo)) {
                throw new RegraNegocioException("A data da medição não pode estar no futuro.");
            }
            Instant instante = m.medidoEm().truncatedTo(ChronoUnit.MILLIS);
            if (medicaoRepository.existsByDispositivoIdAndTipoAndMedidoEm(dispositivoId, m.tipo(), instante)
                    || novas.stream().anyMatch(n -> n.getTipo() == m.tipo() && n.getMedidoEm().equals(instante))) {
                continue;
            }
            MedicaoWearable entidade = new MedicaoWearable();
            entidade.setDispositivo(dispositivo);
            entidade.setUsuario(dispositivo.getUsuario());
            entidade.setTipo(m.tipo());
            entidade.setValor(m.valor().setScale(2, RoundingMode.HALF_UP));
            entidade.setMedidoEm(instante);
            novas.add(entidade);
        }
        medicaoRepository.saveAll(novas);
        dispositivo.setUltimaSincronizacao(Instant.now());
        dispositivoRepository.save(dispositivo);
        return new DispositivoDto.IngestaoResposta(req.medicoes().size(), novas.size());
    }

    /** Última medição de cada tipo (somando todos os dispositivos do usuário). */
    @Transactional(readOnly = true)
    public List<DispositivoDto.MedicaoResposta> ultimasMedicoes(Long usuarioId) {
        List<DispositivoDto.MedicaoResposta> resultado = new ArrayList<>();
        for (TipoMedicao tipo : TipoMedicao.values()) {
            medicaoRepository.findFirstByUsuarioIdAndTipoOrderByMedidoEmDesc(usuarioId, tipo)
                    .ifPresent(m -> resultado.add(new DispositivoDto.MedicaoResposta(
                            m.getTipo(), m.getValor(), m.getTipo().getUnidade(), m.getMedidoEm())));
        }
        return resultado;
    }

    private Dispositivo buscar(Long usuarioId, Long id) {
        return dispositivoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dispositivo não encontrado."));
    }

    private static DispositivoDto.Resposta paraDto(Dispositivo d) {
        return new DispositivoDto.Resposta(d.getId(), d.getNome(), d.getMarca(), d.isConectado(),
                d.getUltimaSincronizacao());
    }
}
