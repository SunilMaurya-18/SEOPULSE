package com.seopulse.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Audit pipeline metrics. Prometheus names: audits_started_total,
 * audits_completed_total{status}, audit_duration_seconds,
 * crawl_pages_total{outcome}, outbox_lag_seconds, redis_stream_pending.
 */
@Component
public class AuditMetrics {

    private final MeterRegistry registry;
    private final Counter auditsStarted;
    private final Timer auditDuration;
    private final AtomicLong outboxLagSeconds = new AtomicLong();
    private final AtomicLong streamPending = new AtomicLong();

    public AuditMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.auditsStarted = Counter.builder("audits.started")
                .description("Audits picked up by a worker")
                .register(registry);
        this.auditDuration = Timer.builder("audit.duration")
                .description("Wall-clock time from worker pickup to a final status")
                .register(registry);

        registry.gauge("outbox.lag.seconds", outboxLagSeconds);
        registry.gauge("redis.stream.pending", streamPending);
    }

    public void auditStarted() {
        auditsStarted.increment();
    }

    public void auditFinished(String status, Duration duration) {
        Counter.builder("audits.completed")
                .description("Audits that reached a final status")
                .tag("status", status)
                .register(registry)
                .increment();

        if (duration != null) {
            auditDuration.record(duration);
        }
    }

    public void pagesCrawled(String outcome, long count) {
        Counter.builder("crawl.pages")
                .description("Pages recorded by the crawler")
                .tag("outcome", outcome)
                .register(registry)
                .increment(count);
    }

    public void outboxLag(long seconds) {
        outboxLagSeconds.set(seconds);
    }

    public void streamPending(long pending) {
        streamPending.set(pending);
    }
}
