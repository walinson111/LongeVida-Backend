package com.longevida.api.service;

import com.longevida.api.dto.UsuarioDto;
import com.longevida.api.entity.ConfiguracaoUsuario;
import com.longevida.api.entity.PerfilProfissional;
import com.longevida.api.entity.Restricao;
import com.longevida.api.entity.Usuario;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.repository.ConfiguracaoUsuarioRepository;
import com.longevida.api.repository.PerfilProfissionalRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final PerfilProfissionalRepository perfilRepository;
    private final ProgressoService progressoService;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConfiguracaoUsuarioRepository configuracaoRepository,
                          PerfilProfissionalRepository perfilRepository,
                          ProgressoService progressoService,
                          Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.perfilRepository = perfilRepository;
        this.progressoService = progressoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UsuarioDto.Resposta obter(Long usuarioId) {
        return montarResposta(buscar(usuarioId));
    }

    @Transactional
    public UsuarioDto.Resposta atualizar(Long usuarioId, UsuarioDto.AtualizarRequest req) {
        Usuario usuario = buscar(usuarioId);
        usuario.setNome(req.nome().trim());
        usuario.setIdade(req.idade());
        usuario.setAlturaCm(req.altura());
        usuario.setNivelAtividade(req.nivelAtividade());
        usuario.getRestricoes().clear();
        if (req.restricoes() != null) {
            usuario.getRestricoes().addAll(new HashSet<>(req.restricoes()));
        }
        usuarioRepository.save(usuario);

        // O peso informado vira o registro de hoje (alimenta o gráfico de evolução)
        // e atualiza o peso atual do usuário.
        progressoService.registrarPeso(usuarioId, req.peso(), LocalDate.now(clock));
        return montarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDto.ConfiguracaoResposta obterConfiguracao(Long usuarioId) {
        return paraDto(buscarConfiguracao(usuarioId));
    }

    @Transactional
    public UsuarioDto.ConfiguracaoResposta atualizarConfiguracao(Long usuarioId, UsuarioDto.ConfiguracaoRequest req) {
        ConfiguracaoUsuario c = buscarConfiguracao(usuarioId);
        c.setNotificacoesAtivas(req.notificacoesAtivas());
        c.setMetaAguaMl(req.metaAguaMl());
        c.setMetaCalorias(req.metaCalorias());
        c.setMetaExercicioMin(req.metaExercicioMin());
        c.setMetaCaloriasGastas(req.metaCaloriasGastas());
        return paraDto(configuracaoRepository.save(c));
    }

    /** Monta o DTO do usuário. Precisa ser chamado dentro de uma transação (coleção lazy). */
    public UsuarioDto.Resposta montarResposta(Usuario u) {
        PerfilProfissional perfil = perfilRepository.findByUsuarioId(u.getId()).orElse(null);
        List<Restricao> restricoes = u.getRestricoes().stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        return new UsuarioDto.Resposta(
                u.getId(), u.getNome(), u.getEmail(), u.getIdade(), u.getPesoKg(), u.getAlturaCm(),
                u.getNivelAtividade(), restricoes, perfil != null, perfil != null ? perfil.getId() : null,
                u.isPerfilSaudeCompleto(), u.getFotoUrl());
    }

    private UsuarioDto.ConfiguracaoResposta paraDto(ConfiguracaoUsuario c) {
        return new UsuarioDto.ConfiguracaoResposta(c.isNotificacoesAtivas(), c.getMetaAguaMl(), c.getMetaCalorias(),
                c.getMetaExercicioMin(), c.getMetaCaloriasGastas());
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private ConfiguracaoUsuario buscarConfiguracao(Long usuarioId) {
        return configuracaoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configurações não encontradas."));
    }
}
