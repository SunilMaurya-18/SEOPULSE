package com.seopulse.website.job;

import com.seopulse.support.AbstractWorkerIntegrationTest;
import com.seopulse.support.TestSite;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.repository.AuditPageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

/**
 * Worker behaviour on failure, timeout, duplicate delivery and crashes.
 * Audits are inserted without an outbox row and processed directly, so
 * the background consumers only see the retries these tests enqueue.
 */
class AuditWorkerIntegrationTest extends AbstractWorkerIntegrationTest {

    @Autowired
    private AuditWorker auditWorker;

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private AuditPageRepository auditPageRepository;

    @Test
    void duplicateDeliveriesProcessTheAuditOnce() throws Exception {

        Audit audit = queuedAudit(createWebsiteFor(simpleSite()));

        CountDownLatch start = new CountDownLatch(1);
        Callable<AuditWorker.Result> worker = () -> {
            start.await();
            return auditWorker.process(audit.getId());
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<AuditWorker.Result> first = pool.submit(worker);
            Future<AuditWorker.Result> second = pool.submit(worker);
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(AuditWorker.Result.COMPLETED, AuditWorker.Result.SKIPPED);
        }

        Audit done = auditRepository.findById(audit.getId()).orElseThrow();
        assertThat(done.getStatus()).isEqualTo(AuditStatus.COMPLETED);
        assertThat(auditPageRepository.findByAuditId(audit.getId())).hasSize(2);
    }

    @Test
    void invalidTargetFailsWithoutRetry() {

        Website website = createWebsiteFor("http://unresolvable-" + UUID.randomUUID() + ".invalid:" + SITE.port() + "/");
        Audit audit = queuedAudit(website);

        assertThat(auditWorker.process(audit.getId())).isEqualTo(AuditWorker.Result.FAILED);

        Audit failed = auditRepository.findById(audit.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AuditStatus.FAILED);
        assertThat(failed.getRetryCount()).isZero();
        assertThat(failed.getErrorMessage()).contains("could not be resolved");
        assertThat(failed.getCompletedAt()).isNotNull();
    }

    @Test
    void transientFailureIsRetriedAndThenSucceeds() throws Exception {

        Audit audit = queuedAudit(createWebsiteFor(simpleSite()));

        doThrow(new IllegalStateException("database hiccup"))
                .doCallRealMethod()
                .when(analysisService).analyzeAudit(audit.getId());

        assertThat(auditWorker.process(audit.getId())).isEqualTo(AuditWorker.Result.RETRY_SCHEDULED);

        // The retry goes through the stream to the background consumer.
        Audit done = awaitAudit(audit.getId(), a -> a.getStatus() == AuditStatus.COMPLETED, Duration.ofSeconds(30));
        assertThat(done.getRetryCount()).isEqualTo(1);
        assertThat(auditPageRepository.findByAuditId(audit.getId())).hasSize(2);
    }

    @Test
    void failsAfterMaxRetries() {

        Audit audit = queuedAudit(createWebsiteFor(simpleSite()));
        audit.setRetryCount(2);
        audit.setMaxRetries(3);
        auditRepository.save(audit);

        doThrow(new IllegalStateException("still broken")).when(analysisService).analyzeAudit(audit.getId());

        assertThat(auditWorker.process(audit.getId())).isEqualTo(AuditWorker.Result.FAILED);

        Audit failed = auditRepository.findById(audit.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AuditStatus.FAILED);
        assertThat(failed.getErrorMessage()).contains("after 3 attempts").contains("still broken");
    }

    @Test
    void auditExceedingTimeLimitFails() {

        String root = SITE.createSite(Map.of(
                "", TestSite.page("Slow", null, "<a href=\"a\">a</a><a href=\"b\">b</a>"),
                "a", TestSite.page("A", null, ""),
                "b", TestSite.page("B", null, "")
        ), Duration.ofSeconds(2));

        Audit audit = queuedAudit(createWebsiteFor(root));

        Duration original = workerProperties.getAuditTimeout();
        workerProperties.setAuditTimeout(Duration.ofSeconds(1));
        try {
            assertThat(auditWorker.process(audit.getId())).isEqualTo(AuditWorker.Result.FAILED);
        } finally {
            workerProperties.setAuditTimeout(original);
        }

        Audit failed = auditRepository.findById(audit.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AuditStatus.FAILED);
        assertThat(failed.getErrorMessage()).contains("time limit");
    }

    @Test
    void reaperRequeuesAuditsAbandonedByDeadWorkers() throws Exception {

        Audit audit = queuedAudit(createWebsiteFor(simpleSite()));
        audit.setStatus(AuditStatus.CRAWLING);
        audit.setStartedAt(Instant.now().minus(Duration.ofHours(2)));
        auditRepository.save(audit);

        auditWorker.reapStuckAudits();

        Audit recovered = awaitAudit(audit.getId(), a -> a.getStatus() == AuditStatus.COMPLETED, Duration.ofSeconds(30));
        assertThat(recovered.getRetryCount()).isEqualTo(1);
    }

    private static String simpleSite() {
        return SITE.createSite(Map.of(
                "", TestSite.page("Worker test home page title here", null, "<h1>Home</h1><a href=\"next\">Next</a>"),
                "next", TestSite.page("Worker test second page title", null, "<h1>Next</h1><a href=\"./\">Home</a>")
        ));
    }
}
