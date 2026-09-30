package com.seopulse.website.job;

import com.seopulse.auth.service.AuthTokenCleanup;
import com.seopulse.common.metrics.AuditMetrics;
import com.seopulse.website.repository.AuditOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Background maintenance that only the worker process runs.
 */
@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class AuditWorkerScheduler {

    private final AuditWorkerRunner runner;
    private final AuditWorker auditWorker;
    private final AuditOutboxRepository outboxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditMetrics metrics;
    private final AuthTokenCleanup authTokenCleanup;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void recoverPendingJobs() {
        run("pending recovery", runner::recoverPendingJobs);
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reapStuckAudits() {
        run("stuck audit reaper", auditWorker::reapStuckAudits);
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 120_000)
    public void cleanupIdleConsumers() {
        run("consumer cleanup", runner::cleanupIdleConsumers);
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 180_000)
    public void cleanupExpiredTokens() {
        run("token cleanup", authTokenCleanup::deleteExpired);
    }

    @Scheduled(fixedDelay = 15_000)
    public void sampleQueueMetrics() {
        run("queue metrics", () -> {
            metrics.outboxLag(outboxRepository.findOldestUnpublishedCreatedAt()
                    .map(oldest -> Math.max(0, Duration.between(oldest, Instant.now()).toSeconds()))
                    .orElse(0L));

            PendingMessagesSummary summary =
                    redisTemplate.opsForStream().pending(AuditQueue.STREAM_KEY, AuditQueue.CONSUMER_GROUP);
            metrics.streamPending(summary == null ? 0 : summary.getTotalPendingMessages());
        });
    }

    private static void run(String task, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error("Scheduled task failed: {}", task, ex);
        }
    }
}
