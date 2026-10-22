package com.longevida.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AtividadeDto {

    private AtividadeDto() {}

    public record Request(
            @NotNull(message = "Selecione um treino.") Long treinoId,
            @NotEmpty(message = "Marque ao menos um exercício.") List<Long> exerciciosConcluidosIds,
            @NotNull(message = "Informe a duração.") @Min(value = 1, message = "Duração mínima: 1 minuto.") @Max(value = 1440, message = "Duração máxima: 1440 minutos.") Integer duracaoMin,
            LocalDate data,
            LocalTime horario) {}

    public record Resposta(
            Long id,
            Long treinoId,
            String nome,
            LocalDate data,
            LocalTime horario,
            Integer duracaoMin,
            Integer caloriasGastas,
            Integer exerciciosConcluidos,
            Integer exerciciosTotal) {}
}
