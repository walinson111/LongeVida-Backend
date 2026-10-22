package com.longevida.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Garante que um usuário não lê, altera nem apaga dados de outro. */
class AutorizacaoIntegrationTest extends IntegrationTestBase {

    private static final String DIETA = """
            {"nome":"Dieta Privada","descricao":"Minha","calorias":2000,"proteinas":150,"carboidratos":200,"gorduras":60}
            """;
    private static final String TREINO = """
            {"nome":"Treino A","objetivo":"Força","nivel":"INICIANTE",
             "exercicios":[{"nome":"Agachamento","series":3,"repeticoes":10,"grupoMuscular":"Pernas"}]}
            """;

    @Test
    void usuarioNaoAcessaDietaDeOutro() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        String beto = cadastrar("Beto", emailUnico());
        long id = idDe(executar("POST", "/api/dietas", ana, DIETA));

        String caminho = "/api/dietas/" + id;
        assertThat(executar("GET", caminho, ana, null).getResponse().getStatus()).isEqualTo(200);
        // 404 (e não 403): nem revela que o recurso existe.
        assertThat(executar("GET", caminho, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("PUT", caminho, beto, DIETA).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("DELETE", caminho, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("POST", caminho + "/publicar", beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(corpo(executar("GET", "/api/dietas", beto, null)).size()).isZero();

        // A dieta de Ana continua intacta.
        assertThat(executar("GET", caminho, ana, null).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void usuarioNaoAcessaTreinoNemRegistraAtividadeComTreinoDeOutro() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        String beto = cadastrar("Beto", emailUnico());
        MvcResult criado = executar("POST", "/api/treinos", ana, TREINO);
        long treinoId = idDe(criado);
        long exercicioId = corpo(criado).get("exercicios").get(0).get("id").asLong();

        assertThat(executar("GET", "/api/treinos/" + treinoId, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("PUT", "/api/treinos/" + treinoId, beto, TREINO).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("DELETE", "/api/treinos/" + treinoId, beto, null).getResponse().getStatus()).isEqualTo(404);

        MvcResult atividade = executar("POST", "/api/atividades", beto,
                """
                {"treinoId":%d,"exerciciosConcluidosIds":[%d],"duracaoMin":30}
                """.formatted(treinoId, exercicioId));
        assertThat(atividade.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void atividadeNaoAceitaExercicioDeOutroTreino() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        long treinoA = idDe(executar("POST", "/api/treinos", ana, TREINO));
        MvcResult outro = executar("POST", "/api/treinos", ana, TREINO);
        long exercicioDoOutro = corpo(outro).get("exercicios").get(0).get("id").asLong();

        MvcResult r = executar("POST", "/api/atividades", ana,
                """
                {"treinoId":%d,"exerciciosConcluidosIds":[%d],"duracaoMin":20}
                """.formatted(treinoA, exercicioDoOutro));
        assertThat(r.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void usuarioNaoAcessaRefeicaoDeOutro() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        String beto = cadastrar("Beto", emailUnico());
        long alimentoId = corpo(executar("GET", "/api/alimentos", ana, null)).get(0).get("id").asLong();
        long refeicaoId = idDe(executar("POST", "/api/refeicoes", ana,
                """
                {"nome":"Almoço","horario":"12:30","itens":[{"alimentoId":%d,"quantidade":100}]}
                """.formatted(alimentoId)));

        assertThat(executar("GET", "/api/refeicoes/" + refeicaoId, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("DELETE", "/api/refeicoes/" + refeicaoId, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(corpo(executar("GET", "/api/refeicoes", beto, null)).size()).isZero();
    }

    @Test
    void dispositivoENotificacaoSaoPrivados() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        String beto = cadastrar("Beto", emailUnico());
        long dispositivoId = idDe(executar("POST", "/api/dispositivos", ana,
                """
                {"nome":"Mi Band 7","marca":"Xiaomi"}
                """));

        assertThat(executar("DELETE", "/api/dispositivos/" + dispositivoId, beto, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("POST", "/api/dispositivos/" + dispositivoId + "/medicoes", beto,
                """
                {"medicoes":[{"tipo":"PASSOS","valor":1000,"medidoEm":"2026-01-01T10:00:00Z"}]}
                """).getResponse().getStatus()).isEqualTo(404);
        assertThat(corpo(executar("GET", "/api/dispositivos", beto, null)).size()).isZero();
    }

    @Test
    void usuarioComumNaoPodePublicar() throws Exception {
        String ana = cadastrar("Ana", emailUnico());
        long id = idDe(executar("POST", "/api/dietas", ana, DIETA));

        assertThat(executar("POST", "/api/dietas/" + id + "/publicar", ana, null).getResponse().getStatus()).isEqualTo(403);
        MvcResult criarPublica = executar("POST", "/api/treinos", ana,
                """
                {"nome":"T","objetivo":"O","nivel":"INICIANTE","publicar":true,"exercicios":[]}
                """);
        assertThat(criarPublica.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void publicarEImportarMantendoCredito() throws Exception {
        String doutor = cadastrarProfissional("Dr. João", emailUnico());
        String maria = cadastrar("Maria", emailUnico());

        long privada = idDe(executar("POST", "/api/dietas", doutor, DIETA));
        long publicada = idDe(executar("POST", "/api/dietas", doutor,
                """
                {"nome":"Mediterrânea","descricao":"D","calorias":1900,"publicar":true}
                """));

        // Dieta privada de outro usuário não é visível nem importável.
        assertThat(executar("GET", "/api/comunidade/dietas/" + privada, maria, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(executar("POST", "/api/comunidade/dietas/" + privada + "/importar", maria, null).getResponse().getStatus()).isEqualTo(404);

        // A publicada é visível e importável.
        MvcResult detalhe = executar("GET", "/api/comunidade/dietas/" + publicada, maria, null);
        assertThat(detalhe.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(detalhe).get("autor").get("nome").asText()).isEqualTo("Dr. João");

        MvcResult copia = executar("POST", "/api/comunidade/dietas/" + publicada + "/importar", maria, null);
        assertThat(copia.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(copia).get("status").asText()).isEqualTo("PRIVADA");
        assertThat(corpo(copia).get("creditoNome").asText()).isEqualTo("Dr. João");
        long copiaId = idDe(copia);
        assertThat(executar("GET", "/api/dietas/" + copiaId, maria, null).getResponse().getStatus()).isEqualTo(200);
        // A cópia é de Maria: o doutor não a enxerga.
        assertThat(executar("GET", "/api/dietas/" + copiaId, doutor, null).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void seguirProfissionalGeraNotificacaoEContagem() throws Exception {
        String doutor = cadastrarProfissional("Dra. Ana", emailUnico());
        String maria = cadastrar("Maria", emailUnico());
        long perfilId = corpo(executar("GET", "/api/profissionais/me", doutor, null)).get("id").asLong();

        assertThat(executar("POST", "/api/comunidade/profissionais/" + perfilId + "/seguir", doutor, null)
                .getResponse().getStatus()).isEqualTo(422);   // não pode seguir a si mesmo

        MvcResult seguiu = executar("POST", "/api/comunidade/profissionais/" + perfilId + "/seguir", maria, null);
        assertThat(seguiu.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(seguiu).get("seguidores").asLong()).isEqualTo(1);
        assertThat(corpo(seguiu).get("seguindo").asBoolean()).isTrue();
        // repetir não duplica
        assertThat(corpo(executar("POST", "/api/comunidade/profissionais/" + perfilId + "/seguir", maria, null))
                .get("seguidores").asLong()).isEqualTo(1);

        MvcResult notificacoes = executar("GET", "/api/notificacoes", doutor, null);
        assertThat(notificacoes.getResponse().getContentAsString()).contains("Novo seguidor");

        MvcResult deixou = executar("DELETE", "/api/comunidade/profissionais/" + perfilId + "/seguir", maria, null);
        assertThat(corpo(deixou).get("seguidores").asLong()).isZero();
        assertThat(corpo(deixou).get("seguindo").asBoolean()).isFalse();
    }
}
