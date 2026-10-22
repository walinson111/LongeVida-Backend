package com.longevida.api.security;

import com.longevida.api.config.AppProperties;
import com.longevida.api.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Emite e valida os tokens JWT (HS256). O segredo vem de JWT_SECRET (Base64, mínimo 32 bytes). */
@Service
public class JwtService {

    private static final String EMISSOR = "longevida-api";
    private static final String CLAIM_VERSAO = "ver";

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(AppProperties props) {
        String segredo = props.jwt().secret();
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException("A variável de ambiente JWT_SECRET é obrigatória.");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(segredo);
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET deve estar em Base64. Gere com: openssl rand -base64 48", e);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET muito curto: use pelo menos 32 bytes (openssl rand -base64 48).");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = props.jwt().expirationMinutes();
    }

    public record TokenEmitido(String token, Instant expiraEm) {}

    public record TokenInfo(long usuarioId, int versao) {}

    public TokenEmitido gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracaoMinutos, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_VERSAO, usuario.getTokenVersion())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /** Devolve os dados do token se a assinatura, o emissor e a validade estiverem corretos. */
    public Optional<TokenInfo> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer versao = claims.get(CLAIM_VERSAO, Integer.class);
            return Optional.of(new TokenInfo(Long.parseLong(claims.getSubject()), versao == null ? 0 : versao));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
