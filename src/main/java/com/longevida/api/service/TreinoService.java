package com.longevida.api.service;

import com.longevida.api.dto.DietaDto;
import com.longevida.api.dto.TreinoDto;
import com.longevida.api.entity.Exercicio;
import com.longevida.api.entity.PerfilProfissional;
import com.longevida.api.entity.Treino;
import com.longevida.api.entity.Visibilidade;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.repository.PerfilProfissionalRepository;
import com.longevida.api.repository.TreinoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreinoService {

    private final TreinoRepository treinoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilProfissionalRepository perfilRepository;
    private final ProfissionalService profissionalService;

    public TreinoService(TreinoRepository treinoRepository,
                         UsuarioRepository usuarioRepository,
                         PerfilProfissionalRepository perfilRepository,
                         ProfissionalService profissionalService) {
        this.treinoRepository = treinoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.profissionalService = profissionalService;
    }

    @Transactional(readOnly = true)
    public List<TreinoDto.Resposta> listar(Long usuarioId) {
        return treinoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream().map(TreinoService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public TreinoDto.Resposta obter(Long usuarioId, Long id) {
        return paraDto(buscar(usuarioId, id));
    }

    @Transactional
    public TreinoDto.Resposta criar(Long usuarioId, TreinoDto.Request req) {
        boolean publicar = Boolean.TRUE.equals(req.publicar());
        PerfilProfissional perfil = publicar ? profissionalService.exigirPerfil(usuarioId) : null;

        Treino treino = new Treino();
        treino.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        aplicar(treino, req);
        if (publicar) {
            treino.setStatus(Visibilidade.PUBLICA);
        }
        treino = treinoRepository.save(treino);

        if (publicar) {
            profissionalService.notificarSeguidoresDePublicacao(perfil, "treino", treino.getNome());
        }
        return paraDto(treino);
    }

    @Transactional
    public TreinoDto.Resposta atualizar(Long usuarioId, Long id, TreinoDto.Request req) {
        Treino treino = buscar(usuarioId, id);
        aplicar(treino, req);
        if (req.publicar() != null) {
            definirPublicacao(usuarioId, treino, req.publicar());
        }
        return paraDto(treinoRepository.save(treino));
    }

    @Transactional
    public TreinoDto.Resposta publicar(Long usuarioId, Long id, boolean publicar) {
        Treino treino = buscar(usuarioId, id);
        definirPublicacao(usuarioId, treino, publicar);
        return paraDto(treinoRepository.save(treino));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        treinoRepository.delete(buscar(usuarioId, id));
    }

    // ------------------------------------------------------------ comunidade

    @Transactional(readOnly = true)
    public TreinoDto.PublicaResposta obterPublico(Long id) {
        Treino t = buscarPublico(id);
        DietaDto.AutorResumo autor = perfilRepository.findByUsuarioId(t.getUsuario().getId())
                .map(p -> new DietaDto.AutorResumo(p.getId(), t.getUsuario().getNome(), p.getEspecialidade()))
                .orElse(new DietaDto.AutorResumo(null, t.getUsuario().getNome(), null));
        return new TreinoDto.PublicaResposta(t.getId(), t.getNome(), t.getObjetivo(), t.getNivel(),
                exerciciosDto(t), autor);
    }

    @Transactional
    public TreinoDto.Resposta importar(Long usuarioId, Long treinoPublicoId) {
        Treino origem = buscarPublico(treinoPublicoId);

        Treino copia = new Treino();
        copia.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        copia.setNome(origem.getNome());
        copia.setObjetivo(origem.getObjetivo());
        copia.setNivel(origem.getNivel());
        copia.setStatus(Visibilidade.PRIVADA);
        copia.setOrigemTreinoId(origem.getId());
        copia.setCreditoNome(origem.getCreditoNome() != null ? origem.getCreditoNome() : origem.getUsuario().getNome());
        for (Exercicio e : origem.getExercicios()) {
            Exercicio novo = new Exercicio();
            novo.setOrdem(e.getOrdem());
            novo.setNome(e.getNome());
            novo.setSeries(e.getSeries());
            novo.setRepeticoes(e.getRepeticoes());
            novo.setGrupoMuscular(e.getGrupoMuscular());
            copia.adicionarExercicio(novo);
        }
        return paraDto(treinoRepository.save(copia));
    }

    // ------------------------------------------------------------ auxiliares

    private void definirPublicacao(Long usuarioId, Treino treino, boolean publicar) {
        if (publicar && treino.getStatus() != Visibilidade.PUBLICA) {
            PerfilProfissional perfil = profissionalService.exigirPerfil(usuarioId);
            treino.setStatus(Visibilidade.PUBLICA);
            profissionalService.notificarSeguidoresDePublicacao(perfil, "treino", treino.getNome());
        } else if (!publicar) {
            treino.setStatus(Visibilidade.PRIVADA);
        }
    }

    private Treino buscar(Long usuarioId, Long id) {
        return treinoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Treino não encontrado."));
    }

    private Treino buscarPublico(Long id) {
        return treinoRepository.buscarPublico(id, Visibilidade.PUBLICA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Treino publicado não encontrado."));
    }

    /** Substitui os exercícios (orphanRemoval apaga os antigos) preservando a ordem enviada. */
    private static void aplicar(Treino t, TreinoDto.Request req) {
        t.setNome(req.nome().trim());
        t.setObjetivo(req.objetivo().trim());
        t.setNivel(req.nivel());
        t.getExercicios().clear();
        if (req.exercicios() != null) {
            int ordem = 0;
            for (TreinoDto.ExercicioRequest e : req.exercicios()) {
                Exercicio ex = new Exercicio();
                ex.setOrdem(ordem++);
                ex.setNome(e.nome().trim());
                ex.setSeries(e.series());
                ex.setRepeticoes(e.repeticoes());
                ex.setGrupoMuscular(e.grupoMuscular() == null ? "" : e.grupoMuscular().trim());
                t.adicionarExercicio(ex);
            }
        }
    }

    private static List<TreinoDto.ExercicioResposta> exerciciosDto(Treino t) {
        return t.getExercicios().stream()
                .map(e -> new TreinoDto.ExercicioResposta(e.getId(), e.getNome(), e.getSeries(), e.getRepeticoes(),
                        e.getGrupoMuscular()))
                .toList();
    }

    private static TreinoDto.Resposta paraDto(Treino t) {
        return new TreinoDto.Resposta(t.getId(), t.getNome(), t.getObjetivo(), t.getNivel(), t.getStatus(),
                t.getCreditoNome(), exerciciosDto(t), t.getCriadoEm());
    }
}
