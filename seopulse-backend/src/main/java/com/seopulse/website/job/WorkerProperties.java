package com.seopulse.website.job;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seopulse.worker")
@Getter
@Setter
public class WorkerProperties {

    /** Audits processed in parallel by one worker process. */
    private int concurrency = 2;

    /** How long one XREADGROUP call blocks waiting for a job. */
    private int blockTimeoutSeconds = 5;

    /** Hard limit for one audit; exceeded audits fail without retry. */
    private Duration auditTimeout = Duration.ofMinutes(30);

    /** How often a running audit checks whether it was cancelled. */
    private long cancellationPollMs = 2000;

    /**
     * Pending stream messages idle longer than this are reclaimed from dead
     * consumers. Must exceed the audit timeout so a live worker's job is
     * never claimed by another.
     */
    private Duration pendingRecoveryIdle = Duration.ofMinutes(35);
    private int pendingRecoveryLimit = 10;

    private int outboxPublishIntervalMs = 2000;
    private int outboxBatchSize = 50;

    /** Consumers idle this long with no pending messages are removed. */
    private int consumerCleanupIdleHours = 24;

    /** Audits running longer than the timeout plus this grace are reaped. */
    private Duration stuckAuditGrace = Duration.ofMinutes(10);
}
