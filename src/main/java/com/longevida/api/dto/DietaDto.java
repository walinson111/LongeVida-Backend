package com.longevida.api.dto;

import com.longevida.api.entity.Visibilidade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class DietaDto {

    private DietaDto() {}

    public record Request(
            @NotBlank(message = "Informe o nome da dieta.") @Size(max = 120, message = "Nome muito longo.") String nome,
            @Size(max = 500, message = "Descrição muito longa.") String descricao,
            @NotNull(message = "Informe a meta calórica.") @Min(value = 0, message = "Calorias inválidas.") @Max(value = 10000, message = "Calorias inválidas.") Integer calorias,
            @DecimalMin(value = "0", message = "Proteínas inválidas.") @DecimalMax(value = "1000", message = "Proteínas inválidas.") @Digits(integer = 4, fraction = 1, message = "Use no máximo 1 casa decimal.") BigDecimal proteinas,
            @DecimalMin(value = "0", message = "Carboidratos inválidos.") @DecimalMax(value = "1000", message = "Carboidratos inválidos.") @Digits(integer = 4, fraction = 1, message = "Use no máximo 1 casa decimal.") BigDecimal carboidratos,
            @DecimalMin(value = "0", message = "Gorduras inválidas.") @DecimalMax(value = "1000", message = "Gorduras inválidas.") @Digits(integer = 4, fraction = 1, message = "Use no máximo 1 casa decimal.") BigDecimal gorduras,
            Boolean publicar) {}

    public record Resposta(
            Long id,
            String nome,
            String descricao,
            Integer calorias,
            BigDecimal proteinas,
            BigDecimal carboidratos,
            BigDecimal gorduras,
            Visibilidade status,
            String creditoNome,
            Instant criadoEm) {}

    public record AutorResumo(Long profissionalId, String nome, String especialidade) {}

    /** Dieta publicada, vista por outros usuários (tela de importação). */
    public record PublicaResposta(
            Long id,
            String nome,
            String descricao,
            Integer calorias,
            BigDecimal proteinas,
            BigDecimal carboidratos,
            BigDecimal gorduras,
            AutorResumo autor) {}
}
