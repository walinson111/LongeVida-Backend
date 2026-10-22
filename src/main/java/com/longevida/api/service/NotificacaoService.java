package com.longevida.api.service;

import com.longevida.api.dto.NotificacaoDto;
import com.longevida.api.entity.Lembrete;
import com.longevida.api.entity.Notificacao;
import com.longevida.api.entity.TipoLembrete;
import com.longevida.api.entity.TipoNotificacao;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.repository.ConfiguracaoUsuarioRepository;
import com.longevida.api.repository.LembreteRepository;
import com.longevida.api.repository.NotificacaoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LembreteRepository lembreteRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              ConfiguracaoUsuarioRepository configuracaoRepository,
                              UsuarioRepository usuarioRepository,
                              LembreteRepository lembreteRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.lembreteRepository = lembreteRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDto.Resposta> listar(Long usuarioId) {
        return notificacaoRepository.findTop100ByUsuarioIdOrderByCriadaEmDesc(usuarioId).stream()
                .map(NotificacaoService::paraDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificacaoDto.Contagem contarNaoLidas(Long usuarioId) {
        return new NotificacaoDto.Contagem(notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId));
    }

    @Transactional
    public NotificacaoDto.Resposta marcarComoLida(Long usuarioId, Long id) {
        Notificacao n = notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."));
        n.setLida(true);
        return paraDto(notificacaoRepository.save(n));
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        notificacaoRepository.marcarTodasComoLidas(usuarioId);
    }

    /** Cria uma notificação para cada usuário que mantém as notificações ligadas. */
    @Transactional
    public void notificar(Collection<Long> usuarioIds, TipoNotificacao tipo, String titulo, String mensagem) {
        if (usuarioIds.isEmpty()) {
            return;
        }
        List<Long> destinatarios = configuracaoRepository.idsComNotificacoesAtivas(usuarioIds);
        List<Notificacao> novas = destinatarios.stream().map(id -> {
            Notificacao n = new Notificacao();
            n.setUsuario(usuarioRepository.getReferenceById(id));
            n.setTipo(tipo);
            n.setTitulo(limitar(titulo, 120));
            n.setMensagem(limitar(mensagem, 300));
            return n;
        }).toList();
        notificacaoRepository.saveAll(novas);
    }

    /**
     * Chamado a cada minuto pelo {@link LembreteScheduler}: cria a notificação dos lembretes
     * que vencem neste minuto (no máximo uma por lembrete por dia).
     */
    @Transactional
    public int gerarLembretes(LocalDateTime agora) {
        LocalTime hora = agora.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        int criadas = 0;
        for (Lembrete l : lembreteRepository.listarAtivosNoHorario(hora)) {
            if (notificacaoRepository.existsByLembreteIdAndReferenciaData(l.getId(), agora.toLocalDate())) {
                continue;
            }
            Notificacao n = new Notificacao();
            n.setUsuario(l.getUsuario());
            n.setTipo(l.getTipo() == TipoLembrete.TREINO ? TipoNotificacao.TREINO : TipoNotificacao.LEMBRETE);
            n.setTitulo(limitar("Lembrete: " + l.getTitulo(), 120));
            n.setMensagem(limitar("Está na hora: " + l.getTitulo() + ".", 300));
            n.setLembreteId(l.getId());
            n.setReferenciaData(agora.toLocalDate());
            notificacaoRepository.save(n);
            criadas++;
        }
        return criadas;
    }

    private static NotificacaoDto.Resposta paraDto(Notificacao n) {
        return new NotificacaoDto.Resposta(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(), n.isLida(),
                n.getCriadaEm());
    }

    static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
