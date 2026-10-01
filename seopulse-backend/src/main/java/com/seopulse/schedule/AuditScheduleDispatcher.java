package com.seopulse.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class AuditScheduleDispatcher {

    private final ScheduleService scheduleService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    @SchedulerLock(name = "audit-schedule-dispatch", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void dispatch() {
        try {
            int processed = scheduleService.dispatchDue();
            if (processed > 0) {
                log.info("Scheduled audits dispatched: {}", processed);
            }
        } catch (RuntimeException ex) {
            log.error("Scheduled audit dispatch failed", ex);
        }
    }
}
