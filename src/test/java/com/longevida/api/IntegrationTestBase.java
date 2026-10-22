package com.longevida.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes de integração: sobe a API inteira contra um PostgreSQL real (Docker).
 * Sem Docker disponível, estes testes são ignorados (disabledWithoutDocker).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected static String emailUnico() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    protected MockHttpServletRequestBuilder requisicao(String metodo, String caminho, String token, String corpo) {
        MockHttpServletRequestBuilder b = switch (metodo) {
            case "POST" -> MockMvcRequestBuilders.post(caminho);
            case "PUT" -> MockMvcRequestBuilders.put(caminho);
            case "DELETE" -> MockMvcRequestBuilders.delete(caminho);
            case "PATCH" -> MockMvcRequestBuilders.patch(caminho);
            default -> MockMvcRequestBuilders.get(caminho);
        };
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return b;
    }

    protected MvcResult executar(String metodo, String caminho, String token, String corpo) throws Exception {
        return mvc.perform(requisicao(metodo, caminho, token, corpo)).andReturn();
    }

    protected JsonNode corpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    /** Cadastra um usuário comum e devolve o token. */
    protected String cadastrar(String nome, String email) throws Exception {
        String body = """
                {"nome":"%s","email":"%s","senha":"senha-forte-123","idade":65}
                """.formatted(nome, email);
        return corpo(executar("POST", "/api/auth/cadastro", null, body)).get("token").asText();
    }

    /** Cadastra um profissional de saúde e devolve o token. */
    protected String cadastrarProfissional(String nome, String email) throws Exception {
        String body = """
                {"nome":"%s","email":"%s","senha":"senha-forte-123","idade":40,
                 "perfilProfissional":{"especialidade":"Nutricionista","registro":"CRN-12345","descricao":"Teste"}}
                """.formatted(nome, email);
        return corpo(executar("POST", "/api/auth/cadastro", null, body)).get("token").asText();
    }

    protected long idDe(MvcResult r) throws Exception {
        return corpo(r).get("id").asLong();
    }

}
