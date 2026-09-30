package com.seopulse.website.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditWorkerScheduler {

    private final AuditWorker auditWorker;

    @Scheduled(fixedDelay = 1000)
    public void processJobs() {
        auditWorker.processNextJob();
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverPendingJobs() {
        auditWorker.recoverPendingJobs();
    }
}