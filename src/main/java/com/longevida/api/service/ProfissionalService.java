package com.longevida.api.service;

import com.longevida.api.dto.ComunidadeDto;
import com.longevida.api.entity.Dieta;
import com.longevida.api.entity.PerfilProfissional;
import com.longevida.api.entity.Seguidor;
import com.longevida.api.entity.TipoNotificacao;
import com.longevida.api.entity.Treino;
import com.longevida.api.entity.Usuario;
import com.longevida.api.entity.Visibilidade;
import com.longevida.api.exception.AcessoNegadoException;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.DietaRepository;
import com.longevida.api.repository.PerfilProfissionalRepository;
import com.longevida.api.repository.SeguidorRepository;
import com.longevida.api.repository.TreinoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfis profissionais, comunidade e seguidores. */
@Service
public class ProfissionalService {

    private final PerfilProfissionalRepository perfilRepository;
    private final SeguidorRepository seguidorRepository;
    private final DietaRepository dietaRepository;
    private final TreinoRepository treinoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public ProfissionalService(PerfilProfissionalRepository perfilRepository,
                               SeguidorRepository seguidorRepository,
                               DietaRepository dietaRepository,
                               TreinoRepository treinoRepository,
                               UsuarioRepository usuarioRepository,
                               NotificacaoService notificacaoService) {
        this.perfilRepository = perfilRepository;
        this.seguidorRepository = seguidorRepository;
        this.dietaRepository = dietaRepository;
        this.treinoRepository = treinoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
    }

    // ------------------------------------------------------------ perfil próprio

    @Transactional(readOnly = true)
    public ComunidadeDto.MeuPerfilResposta meuPerfil(Long usuarioId) {
        return paraMeuPerfil(exigirPerfil(usuarioId));
    }

    @Transactional
    public ComunidadeDto.MeuPerfilResposta atualizarMeuPerfil(Long usuarioId, ComunidadeDto.MeuPerfilRequest req) {
        PerfilProfissional perfil = exigirPerfil(usuarioId);
        perfil.setEspecialidade(req.especialidade().trim());
        perfil.setDescricao(req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().trim());
        return paraMeuPerfil(perfilRepository.save(perfil));
    }

    /** Garante que o usuário é profissional (necessário para publicar conteúdo). */
    @Transactional(readOnly = true)
    public PerfilProfissional exigirPerfil(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AcessoNegadoException("Apenas profissionais de saúde podem publicar conteúdo."));
    }

    // ------------------------------------------------------------ comunidade

    @Transactional(readOnly = true)
    public List<ComunidadeDto.ProfissionalResposta> listar(Long usuarioId) {
        return montar(perfilRepository.listarComUsuario(), usuarioId);
    }

    @Transactional(readOnly = true)
    public ComunidadeDto.ProfissionalResposta detalhar(Long usuarioId, Long perfilId) {
        return montar(List.of(buscarComUsuario(perfilId)), usuarioId).get(0);
    }

    @Transactional
    public ComunidadeDto.ProfissionalResposta seguir(Long usuarioId, Long perfilId) {
        PerfilProfissional perfil = buscarComUsuario(perfilId);
        if (perfil.getUsuario().getId().equals(usuarioId)) {
            throw new RegraNegocioException("Você não pode seguir o seu próprio perfil.");
        }
        if (seguidorRepository.findBySeguidorIdAndProfissionalId(usuarioId, perfilId).isEmpty()) {
            Usuario seguidorUsuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
            Seguidor s = new Seguidor();
            s.setSeguidor(seguidorUsuario);
            s.setProfissional(perfil);
            seguidorRepository.save(s);

            notificacaoService.notificar(List.of(perfil.getUsuario().getId()), TipoNotificacao.SOCIAL,
                    "Novo seguidor", seguidorUsuario.getNome() + " começou a seguir você.");
        }
        return montar(List.of(perfil), usuarioId).get(0);
    }

    @Transactional
    public ComunidadeDto.ProfissionalResposta deixarDeSeguir(Long usuarioId, Long perfilId) {
        PerfilProfissional perfil = buscarComUsuario(perfilId);
        seguidorRepository.findBySeguidorIdAndProfissionalId(usuarioId, perfilId).ifPresent(seguidorRepository::delete);
        seguidorRepository.flush();
        return montar(List.of(perfil), usuarioId).get(0);
    }

    /** Avisa quem segue o profissional que há conteúdo novo. */
    @Transactional
    public void notificarSeguidoresDePublicacao(PerfilProfissional perfil, String tipoConteudo, String nomeConteudo) {
        List<Long> seguidores = seguidorRepository.idsDosSeguidores(perfil.getId());
        String autor = perfil.getUsuario().getNome();
        notificacaoService.notificar(seguidores, TipoNotificacao.SOCIAL, "Nova " + tipoConteudo + " publicada",
                autor + " publicou: " + nomeConteudo);
    }

    // ------------------------------------------------------------ auxiliares

    private PerfilProfissional buscarComUsuario(Long perfilId) {
        return perfilRepository.buscarComUsuario(perfilId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado."));
    }

    private ComunidadeDto.MeuPerfilResposta paraMeuPerfil(PerfilProfissional p) {
        return new ComunidadeDto.MeuPerfilResposta(p.getId(), p.getEspecialidade(), p.getRegistroProfissional(),
                p.getDescricao(), seguidorRepository.countByProfissionalId(p.getId()));
    }

    /** Monta os perfis da comunidade com poucas consultas (sem N+1). */
    private List<ComunidadeDto.ProfissionalResposta> montar(List<PerfilProfissional> perfis, Long usuarioLogadoId) {
        if (perfis.isEmpty()) {
            return List.of();
        }
        List<Long> perfilIds = perfis.stream().map(PerfilProfissional::getId).toList();
        List<Long> usuarioIds = perfis.stream().map(p -> p.getUsuario().getId()).toList();

        Map<Long, Long> seguidores = seguidorRepository.contarPorProfissional(perfilIds).stream()
                .collect(Collectors.toMap(SeguidorRepository.ContagemSeguidores::getProfissionalId,
                        SeguidorRepository.ContagemSeguidores::getTotal));
        Set<Long> seguidos = seguidorRepository.idsSeguidosPor(usuarioLogadoId);
        Map<Long, List<Dieta>> dietas = dietaRepository.listarPorUsuariosEStatus(usuarioIds, Visibilidade.PUBLICA)
                .stream().collect(Collectors.groupingBy(d -> d.getUsuario().getId()));
        Map<Long, List<Treino>> treinos = treinoRepository.listarPorUsuariosEStatus(usuarioIds, Visibilidade.PUBLICA)
                .stream().collect(Collectors.groupingBy(t -> t.getUsuario().getId()));

        return perfis.stream().map(p -> {
            Long donoId = p.getUsuario().getId();
            List<ComunidadeDto.DietaResumo> dietasDto = dietas.getOrDefault(donoId, List.of()).stream()
                    .map(d -> new ComunidadeDto.DietaResumo(d.getId(), d.getNome(), d.getCalorias())).toList();
            List<ComunidadeDto.TreinoResumo> treinosDto = treinos.getOrDefault(donoId, List.of()).stream()
                    .map(t -> new ComunidadeDto.TreinoResumo(t.getId(), t.getNome(), t.getNivel())).toList();
            return new ComunidadeDto.ProfissionalResposta(
                    p.getId(), p.getUsuario().getNome(), p.getEspecialidade(), p.getDescricao(),
                    seguidores.getOrDefault(p.getId(), 0L), p.getUsuario().getFotoUrl(),
                    seguidos.contains(p.getId()), donoId.equals(usuarioLogadoId), dietasDto, treinosDto);
        }).toList();
    }
}
