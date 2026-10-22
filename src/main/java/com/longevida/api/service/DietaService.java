package com.longevida.api.service;

import com.longevida.api.dto.DietaDto;
import com.longevida.api.entity.Dieta;
import com.longevida.api.entity.PerfilProfissional;
import com.longevida.api.entity.Visibilidade;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.repository.DietaRepository;
import com.longevida.api.repository.PerfilProfissionalRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DietaService {

    private final DietaRepository dietaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilProfissionalRepository perfilRepository;
    private final ProfissionalService profissionalService;

    public DietaService(DietaRepository dietaRepository,
                        UsuarioRepository usuarioRepository,
                        PerfilProfissionalRepository perfilRepository,
                        ProfissionalService profissionalService) {
        this.dietaRepository = dietaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.profissionalService = profissionalService;
    }

    @Transactional(readOnly = true)
    public List<DietaDto.Resposta> listar(Long usuarioId) {
        return dietaRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream().map(DietaService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public DietaDto.Resposta obter(Long usuarioId, Long id) {
        return paraDto(buscar(usuarioId, id));
    }

    @Transactional
    public DietaDto.Resposta criar(Long usuarioId, DietaDto.Request req) {
        boolean publicar = Boolean.TRUE.equals(req.publicar());
        PerfilProfissional perfil = publicar ? profissionalService.exigirPerfil(usuarioId) : null;

        Dieta dieta = new Dieta();
        dieta.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        aplicar(dieta, req);
        if (publicar) {
            dieta.setStatus(Visibilidade.PUBLICA);
        }
        dieta = dietaRepository.save(dieta);

        if (publicar) {
            profissionalService.notificarSeguidoresDePublicacao(perfil, "dieta", dieta.getNome());
        }
        return paraDto(dieta);
    }

    @Transactional
    public DietaDto.Resposta atualizar(Long usuarioId, Long id, DietaDto.Request req) {
        Dieta dieta = buscar(usuarioId, id);
        aplicar(dieta, req);
        if (req.publicar() != null) {
            definirPublicacao(usuarioId, dieta, req.publicar());
        }
        return paraDto(dietaRepository.save(dieta));
    }

    @Transactional
    public DietaDto.Resposta publicar(Long usuarioId, Long id, boolean publicar) {
        Dieta dieta = buscar(usuarioId, id);
        definirPublicacao(usuarioId, dieta, publicar);
        return paraDto(dietaRepository.save(dieta));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        dietaRepository.delete(buscar(usuarioId, id));
    }

    // ------------------------------------------------------------ comunidade

    @Transactional(readOnly = true)
    public DietaDto.PublicaResposta obterPublica(Long id) {
        Dieta d = buscarPublica(id);
        return new DietaDto.PublicaResposta(d.getId(), d.getNome(), d.getDescricao(), d.getCalorias(), d.getProteinas(),
                d.getCarboidratos(), d.getGorduras(), autor(d));
    }

    /** Copia uma dieta publicada para o usuário (privada, editável, com crédito ao autor). */
    @Transactional
    public DietaDto.Resposta importar(Long usuarioId, Long dietaPublicaId) {
        Dieta origem = buscarPublica(dietaPublicaId);

        Dieta copia = new Dieta();
        copia.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        copia.setNome(origem.getNome());
        copia.setDescricao(origem.getDescricao());
        copia.setCalorias(origem.getCalorias());
        copia.setProteinas(origem.getProteinas());
        copia.setCarboidratos(origem.getCarboidratos());
        copia.setGorduras(origem.getGorduras());
        copia.setStatus(Visibilidade.PRIVADA);
        copia.setOrigemDietaId(origem.getId());
        // Mantém o crédito do autor original mesmo em cópias de cópias.
        copia.setCreditoNome(origem.getCreditoNome() != null ? origem.getCreditoNome() : origem.getUsuario().getNome());
        return paraDto(dietaRepository.save(copia));
    }

    // ------------------------------------------------------------ auxiliares

    private void definirPublicacao(Long usuarioId, Dieta dieta, boolean publicar) {
        if (publicar && dieta.getStatus() != Visibilidade.PUBLICA) {
            PerfilProfissional perfil = profissionalService.exigirPerfil(usuarioId);
            dieta.setStatus(Visibilidade.PUBLICA);
            profissionalService.notificarSeguidoresDePublicacao(perfil, "dieta", dieta.getNome());
        } else if (!publicar) {
            dieta.setStatus(Visibilidade.PRIVADA);
        }
    }

    private Dieta buscar(Long usuarioId, Long id) {
        return dietaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dieta não encontrada."));
    }

    private Dieta buscarPublica(Long id) {
        return dietaRepository.buscarPublica(id, Visibilidade.PUBLICA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dieta publicada não encontrada."));
    }

    private DietaDto.AutorResumo autor(Dieta d) {
        return perfilRepository.findByUsuarioId(d.getUsuario().getId())
                .map(p -> new DietaDto.AutorResumo(p.getId(), d.getUsuario().getNome(), p.getEspecialidade()))
                .orElse(new DietaDto.AutorResumo(null, d.getUsuario().getNome(), null));
    }

    private static void aplicar(Dieta d, DietaDto.Request req) {
        d.setNome(req.nome().trim());
        d.setDescricao(req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().trim());
        d.setCalorias(req.calorias());
        d.setProteinas(macro(req.proteinas()));
        d.setCarboidratos(macro(req.carboidratos()));
        d.setGorduras(macro(req.gorduras()));
    }

    private static BigDecimal macro(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(1, RoundingMode.HALF_UP);
    }

    private static DietaDto.Resposta paraDto(Dieta d) {
        return new DietaDto.Resposta(d.getId(), d.getNome(), d.getDescricao(), d.getCalorias(), d.getProteinas(),
                d.getCarboidratos(), d.getGorduras(), d.getStatus(), d.getCreditoNome(), d.getCriadoEm());
    }
}
