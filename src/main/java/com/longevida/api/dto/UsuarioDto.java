package com.longevida.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longevida.api.entity.NivelAtividade;
import com.longevida.api.entity.Restricao;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class UsuarioDto {

    private UsuarioDto() {}

    public record Resposta(
            Long id,
            String nome,
            String email,
            Integer idade,
            BigDecimal peso,
            Integer altura,
            NivelAtividade nivelAtividade,
            List<Restricao> restricoes,
            @JsonProperty("isProfissional") boolean profissional,
            Long profissionalId,
            boolean perfilSaudeCompleto,
            String fotoUrl) {}

    /** Usado no "primeiro acesso" e em "editar perfil". */
    public record AtualizarRequest(
            @NotBlank(message = "Informe o nome.") @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.") String nome,
            @NotNull(message = "Informe a idade.") @Min(value = 1, message = "Idade inválida.") @Max(value = 120, message = "Idade inválida.") Integer idade,
            @NotNull(message = "Informe o peso.") @DecimalMin(value = "20.0", message = "Peso inválido.") @DecimalMax(value = "500.0", message = "Peso inválido.") @Digits(integer = 3, fraction = 1, message = "Use no máximo 1 casa decimal no peso.") BigDecimal peso,
            @NotNull(message = "Informe a altura.") @Min(value = 50, message = "Altura inválida.") @Max(value = 250, message = "Altura inválida.") Integer altura,
            @NotNull(message = "Informe o nível de atividade.") NivelAtividade nivelAtividade,
            Set<Restricao> restricoes) {}

    public record ConfiguracaoResposta(
            boolean notificacoesAtivas,
            int metaAguaMl,
            int metaCalorias,
            int metaExercicioMin,
            int metaCaloriasGastas) {}

    public record ConfiguracaoRequest(
            @NotNull Boolean notificacoesAtivas,
            @NotNull(message = "Informe a meta de água.") @Min(value = 1, message = "Meta de água inválida.") @Max(value = 10000, message = "Meta de água inválida.") Integer metaAguaMl,
            @NotNull(message = "Informe a meta de calorias.") @Min(value = 1, message = "Meta de calorias inválida.") @Max(value = 10000, message = "Meta de calorias inválida.") Integer metaCalorias,
            @NotNull(message = "Informe a meta de exercícios.") @Min(value = 1, message = "Meta de exercícios inválida.") @Max(value = 1440, message = "Meta de exercícios inválida.") Integer metaExercicioMin,
            @NotNull(message = "Informe a meta de calorias gastas.") @Min(value = 1, message = "Meta de calorias gastas inválida.") @Max(value = 10000, message = "Meta de calorias gastas inválida.") Integer metaCaloriasGastas) {}
}
