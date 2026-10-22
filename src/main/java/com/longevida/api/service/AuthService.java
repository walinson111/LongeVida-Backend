package com.longevida.api.service;

import com.longevida.api.config.AppProperties;
import com.longevida.api.dto.AuthDto;
import com.longevida.api.dto.UsuarioDto;
import com.longevida.api.entity.ConfiguracaoUsuario;
import com.longevida.api.entity.Lembrete;
import com.longevida.api.entity.PasswordResetToken;
import com.longevida.api.entity.PerfilProfissional;
import com.longevida.api.entity.TipoLembrete;
import com.longevida.api.entity.Usuario;
import com.longevida.api.exception.ConflitoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.ConfiguracaoUsuarioRepository;
import com.longevida.api.repository.LembreteRepository;
import com.longevida.api.repository.PasswordResetTokenRepository;
import com.longevida.api.repository.PerfilProfissionalRepository;
import com.longevida.api.repository.UsuarioRepository;
import com.longevida.api.security.JwtService;
import com.longevida.api.security.UsuarioPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MSG_CODIGO_INVALIDO = "Código inválido ou expirado. Solicite um novo código.";

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final PerfilProfissionalRepository perfilRepository;
    private final LembreteRepository lembreteRepository;
    private final PasswordResetTokenRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final EmailService emailService;
    private final AppProperties props;

    public AuthService(UsuarioRepository usuarioRepository,
                       ConfiguracaoUsuarioRepository configuracaoRepository,
                       PerfilProfissionalRepository perfilRepository,
                       LembreteRepository lembreteRepository,
                       PasswordResetTokenRepository resetRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UsuarioService usuarioService,
                       EmailService emailService,
                       AppProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.perfilRepository = perfilRepository;
        this.lembreteRepository = lembreteRepository;
        this.resetRepository = resetRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.emailService = emailService;
        this.props = props;
    }

    @Transactional
    public AuthDto.AuthResponse cadastrar(AuthDto.CadastroRequest req) {
        String email = normalizarEmail(req.email());
        validarTamanhoSenha(req.senha());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuario.setIdade(req.idade());
        usuario = usuarioRepository.save(usuario);

        ConfiguracaoUsuario configuracao = new ConfiguracaoUsuario();
        configuracao.setUsuario(usuario);
        configuracaoRepository.save(configuracao);

        criarLembretesPadrao(usuario);

        if (req.perfilProfissional() != null) {
            PerfilProfissional perfil = new PerfilProfissional();
            perfil.setUsuario(usuario);
            perfil.setEspecialidade(req.perfilProfissional().especialidade().trim());
            perfil.setRegistroProfissional(req.perfilProfissional().registro().trim());
            perfil.setDescricao(textoOuNulo(req.perfilProfissional().descricao()));
            perfilRepository.save(perfil);
        }

        return montarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        // Lança AuthenticationException (-> 401 genérico) se e-mail/senha estiverem errados.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizarEmail(req.email()), req.senha()));
        Long id = ((UsuarioPrincipal) auth.getPrincipal()).getId();
        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        return montarResposta(usuario);
    }

    /**
     * Gera um código de 6 dígitos e o envia por e-mail. Responde sempre da mesma forma,
     * exista ou não o e-mail, para não revelar quais e-mails estão cadastrados.
     */
    @Transactional
    public void esqueciSenha(AuthDto.EsqueciSenhaRequest req) {
        usuarioRepository.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .filter(Usuario::isAtivo)
                .ifPresent(usuario -> {
                    resetRepository.apagarDoUsuario(usuario.getId());
                    String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUsuario(usuario);
                    token.setCodigoHash(passwordEncoder.encode(codigo));
                    token.setExpiraEm(Instant.now().plus(props.passwordReset().expirationMinutes(), ChronoUnit.MINUTES));
                    resetRepository.save(token);

                    try {
                        emailService.enviarCodigoRecuperacao(usuario.getEmail(), usuario.getNome(), codigo,
                                props.passwordReset().expirationMinutes());
                    } catch (RuntimeException e) {
                        // Não vaza a falha ao cliente (evita enumeração); fica registrada no servidor.
                        log.error("Falha ao enviar e-mail de recuperação de senha", e);
                    }
                });
    }

    /**
     * noRollbackFor: o contador de tentativas erradas precisa ser gravado mesmo
     * quando a requisição termina com erro.
     */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public void redefinirSenha(AuthDto.RedefinirSenhaRequest req) {
        validarTamanhoSenha(req.novaSenha());
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new RegraNegocioException(MSG_CODIGO_INVALIDO));

        PasswordResetToken token = resetRepository
                .findFirstByUsuarioIdAndUsadoEmIsNullOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow(() -> new RegraNegocioException(MSG_CODIGO_INVALIDO));

        if (token.getExpiraEm().isBefore(Instant.now()) || token.getTentativas() >= props.passwordReset().maxAttempts()) {
            throw new RegraNegocioException(MSG_CODIGO_INVALIDO);
        }
        if (!passwordEncoder.matches(req.codigo(), token.getCodigoHash())) {
            token.setTentativas(token.getTentativas() + 1);
            resetRepository.save(token);
            throw new RegraNegocioException(MSG_CODIGO_INVALIDO);
        }

        usuario.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);   // invalida os JWT já emitidos
        usuarioRepository.save(usuario);
        token.setUsadoEm(Instant.now());
        resetRepository.save(token);
    }

    // ---------------------------------------------------------------- auxiliares

    private AuthDto.AuthResponse montarResposta(Usuario usuario) {
        JwtService.TokenEmitido emitido = jwtService.gerar(usuario);
        UsuarioDto.Resposta dto = usuarioService.montarResposta(usuario);
        return new AuthDto.AuthResponse(emitido.token(), "Bearer", emitido.expiraEm(), dto);
    }

    /** Lembretes iniciais (editáveis/desativáveis pelo usuário). */
    private void criarLembretesPadrao(Usuario usuario) {
        List<Lembrete> padrao = List.of(
                novoLembrete(usuario, "Café da manhã", LocalTime.of(8, 0)),
                novoLembrete(usuario, "Almoço", LocalTime.of(12, 0)),
                novoLembrete(usuario, "Jantar", LocalTime.of(19, 0)));
        lembreteRepository.saveAll(padrao);
    }

    private Lembrete novoLembrete(Usuario usuario, String titulo, LocalTime horario) {
        Lembrete l = new Lembrete();
        l.setUsuario(usuario);
        l.setTipo(TipoLembrete.REFEICAO);
        l.setTitulo(titulo);
        l.setHorario(horario);
        l.setAtivo(true);
        return l;
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** BCrypt só considera os primeiros 72 bytes; recusamos senhas maiores em vez de truncar. */
    private static void validarTamanhoSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("A senha é muito longa. Use no máximo 64 caracteres simples.");
        }
    }
}
