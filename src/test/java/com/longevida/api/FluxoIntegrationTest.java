package com.longevida.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;


class FluxoIntegrationTest extends IntegrationTestBase {

    private long alimentoId(String token, String nome) throws Exception {
        for (JsonNode a : corpo(executar("GET", "/api/alimentos", token, null))) {
            if (nome.equals(a.get("nome").asText())) {
                return a.get("id").asLong();
            }
        }
        throw new AssertionError("Alimento não encontrado: " + nome);
    }

    @Test
    void catalogoDeAlimentosFoiCarregadoPelaMigracao() throws Exception {
        String token = cadastrar("Ana", emailUnico());
        JsonNode alimentos = corpo(executar("GET", "/api/alimentos", token, null));
        assertThat(alimentos.size()).isEqualTo(17);
        assertThat(corpo(executar("GET", "/api/alimentos?busca=arroz", token, null)).size()).isEqualTo(1);
    }

    @Test
    void primeiroAcessoAtualizaPerfilEPeso() throws Exception {
        String token = cadastrar("Maria", emailUnico());
        MvcResult r = executar("PUT", "/api/usuarios/me", token,
                """
                {"nome":"Maria Silva","idade":65,"peso":73.8,"altura":165,"nivelAtividade":"MODERADO",
                 "restricoes":["DIABETES","LACTOSE"]}
                """);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode u = corpo(r);
        assertThat(u.get("perfilSaudeCompleto").asBoolean()).isTrue();
        assertThat(u.get("peso").asDouble()).isEqualTo(73.8);
        assertThat(u.get("restricoes")).hasSize(2);

        JsonNode progresso = corpo(executar("GET", "/api/progresso?dias=7", token, null));
        assertThat(progresso.get("pesoAtual").asDouble()).isEqualTo(73.8);
        assertThat(progresso.get("pesos")).hasSize(1);
        assertThat(progresso.get("resumoCorporal").get("imc").asDouble()).isEqualTo(27.1);

       
        assertThat(executar("PUT", "/api/usuarios/me", token,
                """
                {"nome":"Maria","idade":65,"peso":900,"altura":165,"nivelAtividade":"MODERADO"}
                """).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void crudDeDieta() throws Exception {
        String token = cadastrar("Ana", emailUnico());
        MvcResult criada = executar("POST", "/api/dietas", token,
                """
                {"nome":"Low Carb","descricao":"Controle","calorias":1800,"proteinas":120,"carboidratos":100,"gorduras":80}
                """);
        assertThat(criada.getResponse().getStatus()).isEqualTo(201);
        long id = idDe(criada);
        assertThat(corpo(criada).get("status").asText()).isEqualTo("PRIVADA");

        MvcResult editada = executar("PUT", "/api/dietas/" + id, token,
                """
                {"nome":"Low Carb 2","descricao":"Novo","calorias":1700,"proteinas":110.5,"carboidratos":90,"gorduras":70}
                """);
        assertThat(corpo(editada).get("nome").asText()).isEqualTo("Low Carb 2");
        assertThat(corpo(editada).get("proteinas").asDouble()).isEqualTo(110.5);

        assertThat(executar("POST", "/api/dietas", token, "{\"nome\":\"\",\"calorias\":20000}").getResponse().getStatus()).isEqualTo(400);

        assertThat(executar("DELETE", "/api/dietas/" + id, token, null).getResponse().getStatus()).isEqualTo(204);
        assertThat(executar("GET", "/api/dietas/" + id, token, null).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void crudDeTreinoComExercicios() throws Exception {
        String token = cadastrar("Ana", emailUnico());
        MvcResult criado = executar("POST", "/api/treinos", token,
                """
                {"nome":"Força","objetivo":"Massa","nivel":"INTERMEDIARIO","exercicios":[
                  {"nome":"Supino Reto","series":4,"repeticoes":12,"grupoMuscular":"Peito"},
                  {"nome":"Agachamento","series":4,"repeticoes":15,"grupoMuscular":"Pernas"}]}
                """);
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        long id = idDe(criado);
        assertThat(corpo(criado).get("exercicios")).hasSize(2);
        assertThat(corpo(criado).get("exercicios").get(0).get("nome").asText()).isEqualTo("Supino Reto");

        MvcResult editado = executar("PUT", "/api/treinos/" + id, token,
                """
                {"nome":"Força 2","objetivo":"Massa","nivel":"AVANCADO","exercicios":[
                  {"nome":"Remada","series":3,"repeticoes":12,"grupoMuscular":"Costas"}]}
                """);
        assertThat(editado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(editado).get("exercicios")).hasSize(1);
        assertThat(corpo(executar("GET", "/api/treinos/" + id, token, null)).get("exercicios")).hasSize(1);

        
        assertThat(executar("POST", "/api/treinos", token,
                """
                {"nome":"X","objetivo":"Y","nivel":"INICIANTE","exercicios":[{"nome":"A","series":0,"repeticoes":5}]}
                """).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void refeicaoAtividadeResumoHistoricoEProgresso() throws Exception {
        String token = cadastrar("Maria", emailUnico());

        
        long arroz = alimentoId(token, "Arroz Branco");
        long azeite = alimentoId(token, "Azeite");
        MvcResult refeicao = executar("POST", "/api/refeicoes", token,
                """
                {"nome":"Almoço","horario":"12:30","itens":[
                  {"alimentoId":%d,"quantidade":200},{"alimentoId":%d,"quantidade":20}]}
                """.formatted(arroz, azeite));
        assertThat(refeicao.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(refeicao).get("totais").get("calorias").asInt()).isEqualTo(436);

       
        MvcResult treino = executar("POST", "/api/treinos", token,
                """
                {"nome":"Caminhada","objetivo":"Cardio","nivel":"INICIANTE","exercicios":[
                  {"nome":"Caminhada Leve","series":1,"repeticoes":30,"grupoMuscular":"Cardio"},
                  {"nome":"Alongamento","series":1,"repeticoes":10,"grupoMuscular":"Corpo todo"},
                  {"nome":"Respiração","series":1,"repeticoes":5,"grupoMuscular":"Geral"}]}
                """);
        long treinoId = idDe(treino);
        long e1 = corpo(treino).get("exercicios").get(0).get("id").asLong();
        long e2 = corpo(treino).get("exercicios").get(1).get("id").asLong();
        MvcResult atividade = executar("POST", "/api/atividades", token,
                """
                {"treinoId":%d,"exerciciosConcluidosIds":[%d,%d],"duracaoMin":40}
                """.formatted(treinoId, e1, e2));
        assertThat(atividade.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(atividade).get("caloriasGastas").asInt()).isEqualTo(100);
        assertThat(corpo(atividade).get("exerciciosTotal").asInt()).isEqualTo(3);

        // Água
        MvcResult agua = executar("POST", "/api/agua", token, "{\"quantidadeMl\":250}");
        assertThat(corpo(agua).get("totalHojeMl").asLong()).isEqualTo(250);
        assertThat(corpo(executar("POST", "/api/agua", token, "{\"quantidadeMl\":250}")).get("totalHojeMl").asLong()).isEqualTo(500);

        // Metas diárias
        assertThat(executar("PUT", "/api/usuarios/me/configuracoes", token,
                """
                {"notificacoesAtivas":true,"metaAguaMl":2500,"metaCalorias":1900,"metaExercicioMin":45,"metaCaloriasGastas":400}
                """).getResponse().getStatus()).isEqualTo(200);

        JsonNode resumo = corpo(executar("GET", "/api/resumo-diario", token, null));
        assertThat(resumo.get("caloriasConsumidas").asInt()).isEqualTo(436);
        assertThat(resumo.get("caloriasGastas").asInt()).isEqualTo(100);
        assertThat(resumo.get("minutosExercicio").asInt()).isEqualTo(40);
        assertThat(resumo.get("aguaMl").asInt()).isEqualTo(500);
        assertThat(resumo.get("metas").get("aguaMl").asInt()).isEqualTo(2500);
        assertThat(resumo.get("metas").get("calorias").asInt()).isEqualTo(1900);

        JsonNode historico = corpo(executar("GET", "/api/historico", token, null));
        assertThat(historico).hasSize(2);
        assertThat(corpo(executar("GET", "/api/historico?tipo=REFEICAO", token, null))).hasSize(1);
        assertThat(corpo(executar("GET", "/api/historico?tipo=ATIVIDADE", token, null))).hasSize(1);
        assertThat(executar("GET", "/api/historico?tipo=XYZ", token, null).getResponse().getStatus()).isEqualTo(400);

        JsonNode progresso = corpo(executar("GET", "/api/progresso?dias=7", token, null));
        assertThat(progresso.get("calorias")).hasSize(7);
        JsonNode hoje = progresso.get("calorias").get(6);
        assertThat(hoje.get("consumidas").asInt()).isEqualTo(436);
        assertThat(hoje.get("gastas").asInt()).isEqualTo(100);
        assertThat(executar("GET", "/api/progresso?dias=1", token, null).getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void lembretesPadraoENotificacoes() throws Exception {
        String token = cadastrar("Ana", emailUnico());
        JsonNode lembretes = corpo(executar("GET", "/api/lembretes", token, null));
        assertThat(lembretes).hasSize(3);

        long id = lembretes.get(0).get("id").asLong();
        MvcResult desativado = executar("PUT", "/api/lembretes/" + id, token,
                """
                {"tipo":"REFEICAO","titulo":"Café da manhã","horario":"07:30","ativo":false}
                """);
        assertThat(desativado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(desativado).get("ativo").asBoolean()).isFalse();

        assertThat(corpo(executar("GET", "/api/notificacoes/nao-lidas", token, null)).get("naoLidas").asInt()).isZero();
        assertThat(executar("POST", "/api/notificacoes/marcar-todas-lidas", token, null).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void wearablesRecebemMedicoesSemDuplicar() throws Exception {
        String token = cadastrar("Ana", emailUnico());
        long id = idDe(executar("POST", "/api/dispositivos", token,
                """
                {"nome":"Apple Watch Series 9","marca":"Apple"}
                """));
        String lote = """
                {"medicoes":[
                  {"tipo":"PASSOS","valor":3500,"medidoEm":"2026-10-19T12:00:00Z"},
                  {"tipo":"FREQUENCIA_CARDIACA","valor":72,"medidoEm":"2026-10-19T12:00:00Z"}]}
                """;
        JsonNode primeiro = corpo(executar("POST", "/api/dispositivos/" + id + "/medicoes", token, lote));
        assertThat(primeiro.get("gravadas").asInt()).isEqualTo(2);
        JsonNode repetido = corpo(executar("POST", "/api/dispositivos/" + id + "/medicoes", token, lote));
        assertThat(repetido.get("gravadas").asInt()).isZero();

        assertThat(corpo(executar("GET", "/api/dispositivos/medicoes/ultimas", token, null))).hasSize(2);
        assertThat(corpo(executar("GET", "/api/dispositivos", token, null)).get(0).get("ultimaSincronizacao").isNull()).isFalse();

        // valor absurdo é recusado
        assertThat(executar("POST", "/api/dispositivos/" + id + "/medicoes", token,
                """
                {"medicoes":[{"tipo":"FREQUENCIA_CARDIACA","valor":900,"medidoEm":"2026-10-19T13:00:00Z"}]}
                """).getResponse().getStatus()).isEqualTo(422);

        assertThat(executar("PATCH", "/api/dispositivos/" + id + "/conexao", token, "{\"conectado\":false}").getResponse().getStatus()).isEqualTo(200);
        assertThat(executar("POST", "/api/dispositivos/" + id + "/medicoes", token, lote).getResponse().getStatus()).isEqualTo(422);
    }
}
