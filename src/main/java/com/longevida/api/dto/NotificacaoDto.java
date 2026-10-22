package com.longevida.api.dto;

import com.longevida.api.entity.TipoLembrete;
import com.longevida.api.entity.TipoNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;

public final class NotificacaoDto {

    private NotificacaoDto() {}

    public record Resposta(Long id, TipoNotificacao tipo, String titulo, String mensagem, boolean lida, Instant criadaEm) {}

    public record Contagem(long naoLidas) {}

    public record LembreteRequest(
            @NotNull(message = "Informe o tipo do lembrete.") TipoLembrete tipo,
            @NotBlank(message = "Informe o título.") @Size(max = 80, message = "Título muito longo.") String titulo,
            @NotNull(message = "Informe o horário.") LocalTime horario,
            Boolean ativo) {}

    public record LembreteResposta(Long id, TipoLembrete tipo, String titulo, LocalTime horario, boolean ativo) {}
}
