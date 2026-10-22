package com.longevida.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class RefeicaoDto {

    private RefeicaoDto() {}

    public record ItemRequest(
            @NotNull(message = "Informe o alimento.") Long alimentoId,
            @NotNull(message = "Informe a quantidade.") @DecimalMin(value = "0.01", message = "Quantidade inválida.") @DecimalMax(value = "5000", message = "Quantidade inválida.") BigDecimal quantidade) {}

    public record Request(
            @NotBlank(message = "Informe o nome da refeição.") @Size(max = 40, message = "Nome muito longo.") String nome,
            /** Opcional: se ausente, usa a data de hoje. Formato yyyy-MM-dd. */
            LocalDate data,
            @NotNull(message = "Informe o horário.") LocalTime horario,
            @NotEmpty(message = "Adicione ao menos um alimento.") @Size(max = 50, message = "No máximo 50 alimentos.") List<@Valid ItemRequest> itens) {}

    public record ItemResposta(
            Long id,
            Long alimentoId,
            String nome,
            BigDecimal quantidade,
            String unidade,
            Integer calorias,
            BigDecimal proteinas,
            BigDecimal carboidratos,
            BigDecimal gorduras) {}

    public record Totais(int calorias, BigDecimal proteinas, BigDecimal carboidratos, BigDecimal gorduras) {}

    public record Resposta(
            Long id,
            String nome,
            LocalDate data,
            LocalTime horario,
            List<ItemResposta> itens,
            Totais totais) {}
}
