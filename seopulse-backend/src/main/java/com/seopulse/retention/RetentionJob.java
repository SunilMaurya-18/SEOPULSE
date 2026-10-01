package com.seopulse.retention;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class RetentionJob {

    private final RetentionService retentionService;

    @Scheduled(cron = "${seopulse.retention.cron:0 15 2 * * *}", zone = "UTC")
    @SchedulerLock(name = "data-retention", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void run() {
        try {
            retentionService.purge(Instant.now());
        } catch (RuntimeException ex) {
            log.error("Data retention run failed", ex);
        }
    }
}
