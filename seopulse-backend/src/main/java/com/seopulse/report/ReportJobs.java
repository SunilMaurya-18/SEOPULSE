package com.seopulse.report;

import com.seopulse.website.entity.AuditStatus;
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
public class ReportJobs {

    private final ReportGenerator generator;

    @EventListener
    public void onAuditFinished(AuditFinishedEvent event) {
        if (event.status() != AuditStatus.COMPLETED) {
            return;
        }
        try {
            generator.onScheduledAuditCompleted(event.auditId());
        } catch (RuntimeException ex) {
            log.error("Could not queue scheduled report: auditId={}", event.auditId(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${seopulse.reports.poll-interval-ms:5000}", initialDelay = 15_000)
    public void generatePending() {
        try {
            generator.processPending();
        } catch (RuntimeException ex) {
            log.error("Report generation run failed", ex);
        }
    }
}
