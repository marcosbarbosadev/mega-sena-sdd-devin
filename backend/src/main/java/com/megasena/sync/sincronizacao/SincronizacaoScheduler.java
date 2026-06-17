package com.megasena.sync.sincronizacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SincronizacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoScheduler.class);

    private final SincronizacaoService sincronizacaoService;

    public SincronizacaoScheduler(SincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    @Scheduled(cron = "${megasena.sync.cron}")
    public void executarSincronizacaoAgendada() {
        log.info("Disparando sincronização agendada");
        try {
            sincronizacaoService.sincronizar(OrigemSync.AGENDADA);
        } catch (Exception e) {
            log.error("Erro na sincronização agendada: {}", e.getMessage());
        }
    }
}
