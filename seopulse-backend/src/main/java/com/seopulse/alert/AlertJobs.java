package com.seopulse.alert;

import com.seopulse.website.events.AuditFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class AlertJobs {

    private final AlertService alertService;
    private final AlertDispatcher alertDispatcher;

    @EventListener
    public void onAuditFinished(AuditFinishedEvent event) {
        try {
            alertService.evaluate(event.auditId());
        } catch (RuntimeException ex) {
            log.error("Alert evaluation failed: auditId={}", event.auditId(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${seopulse.alerts.dispatch-interval-ms:10000}", initialDelay = 20_000)
    public void dispatch() {
        try {
            alertDispatcher.dispatchPending();
        } catch (RuntimeException ex) {
            log.error("Alert dispatch failed", ex);
        }
    }
}
