package com.longevida.api.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara os lembretes a cada minuto. As notificações ficam disponíveis dentro do app;
 * NÃO há push (isso exigiria Expo Push/FCM/APNs configurados). Ver README.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class LembreteScheduler {

    private static final Logger log = LoggerFactory.getLogger(LembreteScheduler.class);

    private final NotificacaoService notificacaoService;
    private final Clock clock;

    public LembreteScheduler(NotificacaoService notificacaoService, Clock clock) {
        this.notificacaoService = notificacaoService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 * * * * *")
    public void gerarLembretes() {
        try {
            notificacaoService.gerarLembretes(LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            log.error("Falha ao gerar lembretes", e);
        }
    }
}
