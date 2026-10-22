package com.longevida.api.dto;

import com.longevida.api.entity.NivelAtividade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ProgressoDto {

    private ProgressoDto() {}

    public enum TipoHistorico {
        REFEICAO, ATIVIDADE
    }

    public record Metas(int calorias, int aguaMl, int exercicioMin, int caloriasGastas) {}

    public record ResumoDiario(
            LocalDate data,
            long caloriasConsumidas,
            BigDecimal proteinas,
            BigDecimal carboidratos,
            BigDecimal gorduras,
            long caloriasGastas,
            long minutosExercicio,
            long aguaMl,
            Metas metas) {}

    /** tipo: "REFEICAO" ou "ATIVIDADE". */
    public record ItemHistorico(
            String tipo,
            Long id,
            String nome,
            LocalDate data,
            LocalTime horario,
            Integer calorias,
            Integer duracaoMin) {}

    public record PontoPeso(LocalDate data, BigDecimal peso) {}

    public record PontoCalorias(LocalDate data, long consumidas, long gastas) {}

    public record ResumoCorporal(Integer altura, BigDecimal peso, BigDecimal imc, NivelAtividade nivelAtividade) {}

    public record Progresso(
            int dias,
            BigDecimal pesoAtual,
            BigDecimal variacaoPeso,
            List<PontoPeso> pesos,
            List<PontoCalorias> calorias,
            ResumoCorporal resumoCorporal) {}

    public record PesoRequest(
            @NotNull(message = "Informe o peso.") @DecimalMin(value = "20.0", message = "Peso inválido.") @DecimalMax(value = "500.0", message = "Peso inválido.") @Digits(integer = 3, fraction = 1, message = "Use no máximo 1 casa decimal.") BigDecimal peso,
            LocalDate data) {}

    public record AguaRequest(
            @NotNull(message = "Informe a quantidade.") @Min(value = 1, message = "Quantidade inválida.") @Max(value = 5000, message = "Quantidade inválida.") Integer quantidadeMl) {}

    public record AguaResposta(long totalHojeMl, int metaMl) {}
}
