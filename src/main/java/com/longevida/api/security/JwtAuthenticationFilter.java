package com.longevida.api.security;

import com.longevida.api.entity.Usuario;
import com.longevida.api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê "Authorization: Bearer <jwt>", valida o token e carrega o usuário.
 * Token inválido/expirado não gera erro aqui: a requisição segue sem autenticação
 * e o Spring Security responde 401 nas rotas protegidas.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecalho.substring(PREFIXO.length()).trim();
            jwtService.validar(token)
                    .flatMap(info -> usuarioRepository.findById(info.usuarioId())
                            .filter(Usuario::isAtivo)
                            // Senha redefinida => versão mudou => tokens antigos deixam de valer.
                            .filter(u -> u.getTokenVersion() == info.versao()))
                    .ifPresent(usuario -> {
                        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
