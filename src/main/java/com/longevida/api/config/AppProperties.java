package com.longevida.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        Jwt jwt,
        Cors cors,
        Mail mail,
        PasswordReset passwordReset,
        Atividade atividade,
        Scheduler scheduler) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Mail(boolean enabled, String from, boolean logCodigo) {}

    public record PasswordReset(int expirationMinutes, int maxAttempts) {}

    public record Atividade(int kcalPorExercicio) {}

    public record Scheduler(boolean enabled) {}
}
