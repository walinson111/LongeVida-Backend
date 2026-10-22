package com.longevida.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.longevida.api.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    EmailService emailService;

    @Test
    void cadastroLoginEPerfil() throws Exception {
        String email = emailUnico();
        MvcResult cadastro = executar("POST", "/api/auth/cadastro", null,
                """
                {"nome":"Maria Silva","email":"%s","senha":"senha-forte-123","idade":65}
                """.formatted(email.toUpperCase()));
        assertThat(cadastro.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(cadastro).get("token").asText()).isNotBlank();
        assertThat(corpo(cadastro).get("usuario").get("email").asText()).isEqualTo(email); // normalizado

        MvcResult login = executar("POST", "/api/auth/login", null,
                """
                {"email":"%s","senha":"senha-forte-123"}
                """.formatted(email));
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        String token = corpo(login).get("token").asText();

        MvcResult me = executar("GET", "/api/usuarios/me", token, null);
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(me).get("nome").asText()).isEqualTo("Maria Silva");
        assertThat(corpo(me).get("isProfissional").asBoolean()).isFalse();
        assertThat(corpo(me).get("perfilSaudeCompleto").asBoolean()).isFalse();
        // a senha nunca aparece na resposta
        assertThat(me.getResponse().getContentAsString()).doesNotContain("senha").doesNotContain("$2a$");
    }

    @Test
    void emailDuplicadoDa409() throws Exception {
        String email = emailUnico();
        cadastrar("Ana", email);
        MvcResult segundo = executar("POST", "/api/auth/cadastro", null,
                """
                {"nome":"Outra","email":"%s","senha":"senha-forte-123","idade":30}
                """.formatted(email));
        assertThat(segundo.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void validacoesDeCadastro() throws Exception {
        MvcResult senhaCurta = executar("POST", "/api/auth/cadastro", null,
                """
                {"nome":"Ana","email":"%s","senha":"123","idade":30}
                """.formatted(emailUnico()));
        assertThat(senhaCurta.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(senhaCurta).get("campos").has("senha")).isTrue();

        MvcResult emailRuim = executar("POST", "/api/auth/cadastro", null,
                """
                {"nome":"Ana","email":"nao-e-email","senha":"senha-forte-123","idade":30}
                """);
        assertThat(emailRuim.getResponse().getStatus()).isEqualTo(400);

        MvcResult idadeRuim = executar("POST", "/api/auth/cadastro", null,
                """
                {"nome":"Ana","email":"%s","senha":"senha-forte-123","idade":300}
                """.formatted(emailUnico()));
        assertThat(idadeRuim.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void loginComSenhaErradaOuEmailInexistenteDa401Generico() throws Exception {
        String email = emailUnico();
        cadastrar("Ana", email);

        MvcResult errada = executar("POST", "/api/auth/login", null,
                """
                {"email":"%s","senha":"senha-errada-xyz"}
                """.formatted(email));
        MvcResult inexistente = executar("POST", "/api/auth/login", null,
                """
                {"email":"%s","senha":"senha-errada-xyz"}
                """.formatted(emailUnico()));

        assertThat(errada.getResponse().getStatus()).isEqualTo(401);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(401);
        assertThat(corpo(errada).get("mensagem").asText()).isEqualTo(corpo(inexistente).get("mensagem").asText());
    }

    @Test
    void rotasProtegidasExigemTokenValido() throws Exception {
        assertThat(executar("GET", "/api/dietas", null, null).getResponse().getStatus()).isEqualTo(401);
        assertThat(executar("GET", "/api/dietas", "token.invalido.aqui", null).getResponse().getStatus()).isEqualTo(401);

        String token = cadastrar("Ana", emailUnico());
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";
        assertThat(executar("GET", "/api/dietas", adulterado, null).getResponse().getStatus()).isEqualTo(401);
        assertThat(executar("GET", "/api/dietas", token, null).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void recuperacaoDeSenhaCompleta() throws Exception {
        String email = emailUnico();
        String tokenAntigo = cadastrar("Ana", email);

        // E-mail inexistente responde igual (não revela quem tem conta).
        MvcResult inexistente = executar("POST", "/api/auth/esqueci-senha", null,
                """
                {"email":"%s"}
                """.formatted(emailUnico()));
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(200);

        MvcResult pedido = executar("POST", "/api/auth/esqueci-senha", null,
                """
                {"email":"%s"}
                """.formatted(email));
        assertThat(pedido.getResponse().getStatus()).isEqualTo(200);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoRecuperacao(eq(email), org.mockito.ArgumentMatchers.anyString(),
                codigo.capture(), anyInt());
        String codigoCorreto = codigo.getValue();
        assertThat(codigoCorreto).matches("\\d{6}");

        String errado = codigoCorreto.equals("000000") ? "111111" : "000000";
        MvcResult codigoErrado = executar("POST", "/api/auth/redefinir-senha", null,
                """
                {"email":"%s","codigo":"%s","novaSenha":"nova-senha-456"}
                """.formatted(email, errado));
        assertThat(codigoErrado.getResponse().getStatus()).isEqualTo(422);

        MvcResult ok = executar("POST", "/api/auth/redefinir-senha", null,
                """
                {"email":"%s","codigo":"%s","novaSenha":"nova-senha-456"}
                """.formatted(email, codigoCorreto));
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);

        // O código não pode ser reutilizado.
        MvcResult reuso = executar("POST", "/api/auth/redefinir-senha", null,
                """
                {"email":"%s","codigo":"%s","novaSenha":"outra-senha-789"}
                """.formatted(email, codigoCorreto));
        assertThat(reuso.getResponse().getStatus()).isEqualTo(422);

        // Senha antiga não entra mais; a nova sim; tokens antigos foram invalidados.
        assertThat(executar("POST", "/api/auth/login", null,
                """
                {"email":"%s","senha":"senha-forte-123"}
                """.formatted(email)).getResponse().getStatus()).isEqualTo(401);
        assertThat(executar("POST", "/api/auth/login", null,
                """
                {"email":"%s","senha":"nova-senha-456"}
                """.formatted(email)).getResponse().getStatus()).isEqualTo(200);
        assertThat(executar("GET", "/api/usuarios/me", tokenAntigo, null).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void codigoDeRecuperacaoBloqueiaAposMuitasTentativas() throws Exception {
        String email = emailUnico();
        cadastrar("Ana", email);
        executar("POST", "/api/auth/esqueci-senha", null,
                """
                {"email":"%s"}
                """.formatted(email));
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoRecuperacao(eq(email), org.mockito.ArgumentMatchers.anyString(),
                codigo.capture(), anyInt());
        String certo = codigo.getValue();
        String errado = certo.equals("000000") ? "111111" : "000000";

        // application-test.yml: max-attempts = 3
        for (int i = 0; i < 3; i++) {
            executar("POST", "/api/auth/redefinir-senha", null,
                    """
                    {"email":"%s","codigo":"%s","novaSenha":"nova-senha-456"}
                    """.formatted(email, errado));
        }
        MvcResult comCodigoCerto = executar("POST", "/api/auth/redefinir-senha", null,
                """
                {"email":"%s","codigo":"%s","novaSenha":"nova-senha-456"}
                """.formatted(email, certo));
        assertThat(comCodigoCerto.getResponse().getStatus()).isEqualTo(422);
    }
}
