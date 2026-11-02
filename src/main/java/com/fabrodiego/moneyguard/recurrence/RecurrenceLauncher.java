package com.fabrodiego.moneyguard.recurrence;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Lanca as assinaturas do dia: todo dia 00:05 (Brasilia) e ao subir a API, para recuperar dias perdidos. */
@Component
public class RecurrenceLauncher {

    private final RecurrenceService service;

    public RecurrenceLauncher(RecurrenceService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void daily() {
        service.launchAllDue();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        service.launchAllDue();
    }
}
