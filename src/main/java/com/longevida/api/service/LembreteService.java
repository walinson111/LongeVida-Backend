package com.longevida.api.service;

import com.longevida.api.dto.NotificacaoDto;
import com.longevida.api.entity.Lembrete;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.LembreteRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LembreteService {

    private static final int LIMITE_POR_USUARIO = 20;

    private final LembreteRepository lembreteRepository;
    private final UsuarioRepository usuarioRepository;

    public LembreteService(LembreteRepository lembreteRepository, UsuarioRepository usuarioRepository) {
        this.lembreteRepository = lembreteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDto.LembreteResposta> listar(Long usuarioId) {
        return lembreteRepository.findByUsuarioIdOrderByHorarioAsc(usuarioId).stream().map(LembreteService::paraDto).toList();
    }

    @Transactional
    public NotificacaoDto.LembreteResposta criar(Long usuarioId, NotificacaoDto.LembreteRequest req) {
        if (lembreteRepository.countByUsuarioId(usuarioId) >= LIMITE_POR_USUARIO) {
            throw new RegraNegocioException("Limite de " + LIMITE_POR_USUARIO + " lembretes atingido.");
        }
        Lembrete l = new Lembrete();
        l.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        aplicar(l, req);
        return paraDto(lembreteRepository.save(l));
    }

    @Transactional
    public NotificacaoDto.LembreteResposta atualizar(Long usuarioId, Long id, NotificacaoDto.LembreteRequest req) {
        Lembrete l = buscar(usuarioId, id);
        aplicar(l, req);
        return paraDto(lembreteRepository.save(l));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        lembreteRepository.delete(buscar(usuarioId, id));
    }

    private Lembrete buscar(Long usuarioId, Long id) {
        return lembreteRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lembrete não encontrado."));
    }

    private static void aplicar(Lembrete l, NotificacaoDto.LembreteRequest req) {
        l.setTipo(req.tipo());
        l.setTitulo(req.titulo().trim());
        l.setHorario(req.horario().truncatedTo(ChronoUnit.MINUTES));
        l.setAtivo(req.ativo() == null || req.ativo());
    }

    private static NotificacaoDto.LembreteResposta paraDto(Lembrete l) {
        return new NotificacaoDto.LembreteResposta(l.getId(), l.getTipo(), l.getTitulo(), l.getHorario(), l.isAtivo());
    }
}
