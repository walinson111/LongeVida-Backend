package com.longevida.api.dto;

import com.longevida.api.entity.TipoMedicao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DispositivoDto {

    private DispositivoDto() {}

    public record Request(
            @NotBlank(message = "Informe o nome do dispositivo.") @Size(max = 80, message = "Nome muito longo.") String nome,
            @NotBlank(message = "Informe a marca.") @Size(max = 40, message = "Marca muito longa.") String marca,
            @Size(max = 120, message = "Identificador muito longo.") String identificadorExterno) {}

    public record Resposta(Long id, String nome, String marca, boolean conectado, Instant ultimaSincronizacao) {}

    public record ConexaoRequest(@NotNull(message = "Informe o estado da conexão.") Boolean conectado) {}

    public record MedicaoRequest(
            @NotNull(message = "Informe o tipo da medição.") TipoMedicao tipo,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0", message = "Valor inválido.") BigDecimal valor,
            @NotNull(message = "Informe quando foi medido.") Instant medidoEm) {}

    public record MedicoesRequest(
            @NotEmpty(message = "Envie ao menos uma medição.") @Size(max = 500, message = "No máximo 500 medições por envio.") List<@Valid MedicaoRequest> medicoes) {}

    public record IngestaoResposta(int recebidas, int gravadas) {}

    public record MedicaoResposta(TipoMedicao tipo, BigDecimal valor, String unidade, Instant medidoEm) {}
}
