package com.longevida.api.dto;

import com.longevida.api.entity.NivelTreino;
import com.longevida.api.entity.Visibilidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class TreinoDto {

    private TreinoDto() {}

    public record ExercicioRequest(
            @NotBlank(message = "Informe o nome do exercício.") @Size(max = 120, message = "Nome do exercício muito longo.") String nome,
            @NotNull(message = "Informe as séries.") @Min(value = 1, message = "Séries: mínimo 1.") @Max(value = 99, message = "Séries: máximo 99.") Integer series,
            @NotNull(message = "Informe as repetições.") @Min(value = 1, message = "Repetições: mínimo 1.") @Max(value = 999, message = "Repetições: máximo 999.") Integer repeticoes,
            @Size(max = 60, message = "Grupo muscular muito longo.") String grupoMuscular) {}

    public record Request(
            @NotBlank(message = "Informe o nome do treino.") @Size(max = 120, message = "Nome muito longo.") String nome,
            @NotBlank(message = "Informe o objetivo.") @Size(max = 120, message = "Objetivo muito longo.") String objetivo,
            @NotNull(message = "Informe o nível.") NivelTreino nivel,
            Boolean publicar,
            @Valid @Size(max = 50, message = "No máximo 50 exercícios.") List<ExercicioRequest> exercicios) {}

    public record ExercicioResposta(Long id, String nome, Integer series, Integer repeticoes, String grupoMuscular) {}

    public record Resposta(
            Long id,
            String nome,
            String objetivo,
            NivelTreino nivel,
            Visibilidade status,
            String creditoNome,
            List<ExercicioResposta> exercicios,
            Instant criadoEm) {}

    public record PublicaResposta(
            Long id,
            String nome,
            String objetivo,
            NivelTreino nivel,
            List<ExercicioResposta> exercicios,
            DietaDto.AutorResumo autor) {}
}
