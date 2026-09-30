package com.seopulse.website.job;

import com.seopulse.common.metrics.AuditMetrics;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.events.AuditEventPublisher;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.seo.service.AuditAnalysisService;
import com.seopulse.website.service.AuditCrawlerService;
import com.seopulse.website.service.AuditStateChangedException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs one audit through crawl and analysis. Safe to run on many worker
 * processes at once: the QUEUED to CRAWLING claim is an atomic update, so
 * a job delivered twice is processed once.
 */
@Component
@Slf4j
public class AuditWorker {

    public enum Result {
        COMPLETED,
        CANCELLED,
        FAILED,
        SKIPPED,
        RETRY_SCHEDULED,
        /** The retry could not be enqueued; keep the stream message pending. */
        RETRY_PENDING
    }

    static final Set<AuditStatus> RUNNING = EnumSet.of(AuditStatus.CRAWLING, AuditStatus.ANALYZING);

    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final AuditQueue auditQueue;
    private final AuditCrawlerService auditCrawlerService;
    private final AuditAnalysisService auditAnalysisService;
    private final WorkerProperties properties;
    private final AuditMetrics metrics;
    private final AuditEventPublisher events;
    private final TransactionTemplate transactionTemplate;

    private final ExecutorService pipelineExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public AuditWorker(
            AuditRepository auditRepository,
            AuditPageRepository auditPageRepository,
            SeoIssueRepository seoIssueRepository,
            AuditQueue auditQueue,
            AuditCrawlerService auditCrawlerService,
            AuditAnalysisService auditAnalysisService,
            WorkerProperties properties,
            AuditMetrics metrics,
            AuditEventPublisher events,
            TransactionTemplate transactionTemplate
    ) {
        this.auditRepository = auditRepository;
        this.auditPageRepository = auditPageRepository;
        this.seoIssueRepository = seoIssueRepository;
        this.auditQueue = auditQueue;
        this.auditCrawlerService = auditCrawlerService;
        this.auditAnalysisService = auditAnalysisService;
        this.properties = properties;
        this.metrics = metrics;
        this.events = events;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Handles one stream record.
     *
     * @return true when the record may be acknowledged
     */
    public boolean handle(MapRecord<String, Object, Object> record) {

        Object value = record.getValue().get("auditId");
        Long auditId;

        try {
            auditId = Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.error("Discarding audit job with invalid auditId: recordId={}, value={}", record.getId(), value);
            return true;
        }

        MDC.put("auditId", String.valueOf(auditId));
        auditRepository.findOwnerIdById(auditId)
                .ifPresent(userId -> MDC.put("userId", String.valueOf(userId)));
        try {
            return process(auditId) != Result.RETRY_PENDING;
        } finally {
            MDC.remove("auditId");
            MDC.remove("userId");
        }
    }

    public Result process(Long auditId) {

        Instant startedAt = Instant.now();

        if (auditRepository.claim(auditId, AuditStatus.QUEUED, AuditStatus.CRAWLING, startedAt) == 0) {
            log.info(
                    "Skipping audit job: auditId={}, status={}",
                    auditId,
                    auditRepository.findStatusById(auditId).map(Enum::name).orElse("deleted")
            );
            return Result.SKIPPED;
        }

        log.info("Audit claimed: auditId={}", auditId);
        metrics.auditStarted();
        events.publish(auditId, AuditStatus.CRAWLING);

        Map<String, String> mdc = MDC.getCopyOfContextMap();
        Future<?> pipeline = pipelineExecutor.submit(() -> {
            if (mdc != null) {
                MDC.setContextMap(mdc);
            }
            try {
                auditCrawlerService.crawlAudit(auditId);
                events.publish(auditId, AuditStatus.ANALYZING);
                auditAnalysisService.analyzeAudit(auditId);
                return null;
            } finally {
                MDC.clear();
            }
        });

        Duration timeout = properties.getAuditTimeout();
        long deadline = System.nanoTime() + timeout.toNanos();

        try {
            while (true) {
                try {
                    pipeline.get(properties.getCancellationPollMs(), TimeUnit.MILLISECONDS);
                    return finished(auditId, Result.COMPLETED, AuditStatus.COMPLETED, startedAt);
                } catch (TimeoutException ignored) {
                    AuditStatus status = auditRepository.findStatusById(auditId).orElse(AuditStatus.CANCELLED);

                    if (!RUNNING.contains(status)) {
                        pipeline.cancel(true);
                        log.info("Audit stopped while running: auditId={}, status={}", auditId, status);
                        return finished(auditId, Result.CANCELLED, status, startedAt);
                    }

                    if (System.nanoTime() - deadline > 0) {
                        pipeline.cancel(true);
                        fail(auditId, "Audit exceeded the time limit of " + describe(timeout));
                        log.warn("Audit timed out: auditId={}", auditId);
                        return finished(auditId, Result.FAILED, AuditStatus.FAILED, startedAt);
                    }
                }
            }
        } catch (ExecutionException ex) {
            return handleFailure(auditId, ex.getCause(), startedAt);
        } catch (InterruptedException ex) {
            // Worker shutdown: hand the audit to another worker without spending a retry.
            pipeline.cancel(true);
            Thread.currentThread().interrupt();
            log.warn("Worker stopping; re-queueing audit {}", auditId);
            int retryCount = auditRepository.findById(auditId).map(Audit::getRetryCount).orElse(0);
            return requeue(auditId, retryCount, "Worker restarted; audit re-queued")
                    ? Result.RETRY_SCHEDULED
                    : Result.RETRY_PENDING;
        }
    }

    /**
     * Retries or fails audits whose worker died without finishing them.
     */
    public void reapStuckAudits() {

        Instant cutoff = Instant.now()
                .minus(properties.getAuditTimeout())
                .minus(properties.getStuckAuditGrace());

        for (Long auditId : auditRepository.findStuckIds(RUNNING, cutoff)) {
            log.warn("Reaping stuck audit: auditId={}", auditId);
            handleFailure(auditId, new IllegalStateException("The worker stopped before the audit finished"), null);
        }
    }

    private Result handleFailure(Long auditId, Throwable cause, Instant startedAt) {

        if (cause instanceof AuditStateChangedException) {
            AuditStatus status = auditRepository.findStatusById(auditId).orElse(AuditStatus.CANCELLED);
            log.info("Audit changed state during processing: auditId={}, status={}", auditId, status);
            return finished(auditId, Result.CANCELLED, status, startedAt);
        }

        Audit audit = auditRepository.findById(auditId).orElse(null);
        if (audit == null) {
            return Result.SKIPPED;
        }

        String reason = describe(cause);
        int attempt = audit.getRetryCount() + 1;
        boolean retryable = !(cause instanceof IllegalArgumentException);

        if (!retryable || attempt >= audit.getMaxRetries()) {
            log.error("Audit failed permanently: auditId={}, attempts={}", auditId, attempt, cause);
            fail(auditId, retryable
                    ? "Audit failed after " + attempt + " attempts: " + reason
                    : reason);
            return finished(auditId, Result.FAILED, AuditStatus.FAILED, startedAt);
        }

        log.warn("Audit attempt failed; retrying: auditId={}, attempt={}/{}", auditId, attempt, audit.getMaxRetries(), cause);

        boolean enqueued = requeue(auditId, attempt, "Attempt " + attempt + " failed: " + reason + ". Retrying.");
        events.publish(auditId, AuditStatus.QUEUED);
        return enqueued ? Result.RETRY_SCHEDULED : Result.RETRY_PENDING;
    }

    /**
     * Returns the audit to QUEUED with a clean slate and enqueues a new job.
     *
     * @return false if the job could not be enqueued (the DB row is still
     * QUEUED, so pending-message recovery will pick it up)
     */
    private boolean requeue(Long auditId, int retryCount, String message) {

        Integer updated = transactionTemplate.execute(status -> {
            int rows = auditRepository.requeue(auditId, RUNNING, AuditStatus.QUEUED, retryCount, truncate(message));
            if (rows > 0) {
                seoIssueRepository.deleteByAuditId(auditId);
                auditPageRepository.deleteByAuditId(auditId);
            }
            return rows;
        });

        if (updated == null || updated == 0) {
            return true;
        }

        try {
            auditQueue.enqueue(auditId);
            return true;
        } catch (RuntimeException ex) {
            log.error("Failed to enqueue audit retry: auditId={}", auditId, ex);
            return false;
        }
    }

    private void fail(Long auditId, String message) {
        auditRepository.finish(auditId, RUNNING, AuditStatus.FAILED, Instant.now(), truncate(message));
    }

    private Result finished(Long auditId, Result result, AuditStatus status, Instant startedAt) {
        metrics.auditFinished(status.name(), startedAt == null ? null : Duration.between(startedAt, Instant.now()));
        events.publish(auditId, status);
        return result;
    }

    private static String describe(Throwable cause) {
        String message = cause == null ? null : cause.getMessage();
        return message == null || message.isBlank()
                ? "Unexpected error while processing the audit"
                : message;
    }

    private static String describe(Duration duration) {
        return duration.toMinutes() >= 1
                ? duration.toMinutes() + " minutes"
                : duration.toSeconds() + " seconds";
    }

    private static String truncate(String message) {
        return message != null && message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    @PreDestroy
    void shutdown() {
        pipelineExecutor.shutdownNow();
    }
}
