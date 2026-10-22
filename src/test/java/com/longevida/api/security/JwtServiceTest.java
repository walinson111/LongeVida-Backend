package com.longevida.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.longevida.api.config.AppProperties;
import com.longevida.api.entity.Usuario;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SEGREDO = Base64.getEncoder()
            .encodeToString("0123456789-0123456789-0123456789-ab".getBytes());

    private static AppProperties props(String segredo, long minutos) {
        return new AppProperties("America/Sao_Paulo",
                new AppProperties.Jwt(segredo, minutos),
                new AppProperties.Cors(List.of()),
                new AppProperties.Mail(false, "x@x.com", false),
                new AppProperties.PasswordReset(15, 5),
                new AppProperties.Atividade(50),
                new AppProperties.Scheduler(false));
    }

    private static Usuario usuario(long id, int versao) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTokenVersion(versao);
        return u;
    }

    @Test
    void geraEValidaToken() {
        JwtService jwt = new JwtService(props(SEGREDO, 60));
        String token = jwt.gerar(usuario(42L, 3)).token();

        var info = jwt.validar(token);

        assertThat(info).isPresent();
        assertThat(info.get().usuarioId()).isEqualTo(42L);
        assertThat(info.get().versao()).isEqualTo(3);
    }

    @Test
    void rejeitaTokenAdulterado() {
        JwtService jwt = new JwtService(props(SEGREDO, 60));
        String token = jwt.gerar(usuario(1L, 0)).token();
        String adulterado = token.substring(0, token.length() - 3) + (token.endsWith("abc") ? "xyz" : "abc");

        assertThat(jwt.validar(adulterado)).isEmpty();
        assertThat(jwt.validar("isto.nao.e-um-jwt")).isEmpty();
        assertThat(jwt.validar("")).isEmpty();
    }

    @Test
    void rejeitaTokenExpirado() {
        JwtService jwt = new JwtService(props(SEGREDO, -5));
        assertThat(jwt.validar(jwt.gerar(usuario(1L, 0)).token())).isEmpty();
    }

    @Test
    void rejeitaTokenAssinadoComOutroSegredo() {
        String outro = Base64.getEncoder().encodeToString("outro-segredo-outro-segredo-outro-seg".getBytes());
        String token = new JwtService(props(outro, 60)).gerar(usuario(1L, 0)).token();

        assertThat(new JwtService(props(SEGREDO, 60)).validar(token)).isEmpty();
    }

    @Test
    void recusaSegredoAusenteCurtoOuInvalido() {
        assertThatThrownBy(() -> new JwtService(props("", 60))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(null, 60))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(Base64.getEncoder().encodeToString("curto".getBytes()), 60)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("curto");
        assertThatThrownBy(() -> new JwtService(props("%%%nao-e-base64%%%", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
