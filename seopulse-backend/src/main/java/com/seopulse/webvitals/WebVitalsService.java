package com.seopulse.webvitals;

import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.events.AuditFinishedEvent;
import com.seopulse.website.repository.AuditRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Measures the homepage with PageSpeed Insights after each completed audit.
 * Runs in the background because a Lighthouse run takes 10 to 60 seconds.
 */
@Service
@Slf4j
public class WebVitalsService {

    private final WebVitalsProperties properties;
    private final PageSpeedClient pageSpeedClient;
    private final AuditWebVitalsRepository repository;
    private final AuditRepository auditRepository;
    private final ProjectAccessService projectAccessService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore permits;

    public WebVitalsService(
            WebVitalsProperties properties,
            PageSpeedClient pageSpeedClient,
            AuditWebVitalsRepository repository,
            AuditRepository auditRepository,
            ProjectAccessService projectAccessService
    ) {
        this.properties = properties;
        this.pageSpeedClient = pageSpeedClient;
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.projectAccessService = projectAccessService;
        this.permits = new Semaphore(Math.max(1, properties.getMaxConcurrent()));
    }

    @EventListener
    public void onAuditFinished(AuditFinishedEvent event) {
        if (!properties.isEnabled() || event.status() != AuditStatus.COMPLETED) {
            return;
        }
        Audit audit = auditRepository.findByIdWithWebsite(event.auditId()).orElse(null);
        if (audit == null) {
            return;
        }
        String url = audit.getWebsite().getUrl();
        repository.save(AuditWebVitals.builder()
                .auditId(audit.getId())
                .status(AuditWebVitals.Status.PENDING)
                .strategy(properties.getStrategy())
                .url(url)
                .createdAt(Instant.now())
                .build());
        executor.submit(() -> measure(audit.getId(), url));
    }

    void measure(Long auditId, String url) {
        AuditWebVitals row = repository.findById(auditId).orElse(null);
        if (row == null) {
            return;
        }
        try {
            permits.acquire();
            try {
                PageSpeedClient.Measurement m = pageSpeedClient.measure(url);
                row.setPerformanceScore(m.performanceScore());
                row.setLabLcpMs(m.labLcpMs());
                row.setLabCls(m.labCls());
                row.setLabTbtMs(m.labTbtMs());
                row.setLabFcpMs(m.labFcpMs());
                row.setLabSpeedIndexMs(m.labSpeedIndexMs());
                row.setFieldLcpMs(m.fieldLcpMs());
                row.setFieldCls(m.fieldCls());
                row.setFieldInpMs(m.fieldInpMs());
                row.setFieldCategory(m.fieldCategory());
                row.setStatus(AuditWebVitals.Status.READY);
            } finally {
                permits.release();
            }
        } catch (PageSpeedClient.PageSpeedException ex) {
            log.info("Web vitals unavailable: auditId={}, reason={}", auditId, ex.getMessage());
            row.setStatus(AuditWebVitals.Status.FAILED);
            row.setErrorMessage(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (RuntimeException ex) {
            log.warn("Web vitals measurement failed: auditId={}", auditId, ex);
            row.setStatus(AuditWebVitals.Status.FAILED);
            row.setErrorMessage("Measurement failed unexpectedly.");
        }
        row.setMeasuredAt(Instant.now());
        if (repository.existsById(auditId)) {
            repository.save(row);
        }
    }

    public WebVitalsResponse get(Long projectId, Long auditId, Long userId) {
        projectAccessService.requireOwnedAudit(projectId, auditId, userId);

        AuditWebVitals row = repository.findById(auditId).orElse(null);
        if (row == null) {
            return WebVitalsResponse.of(WebVitalsResponse.State.UNAVAILABLE);
        }
        WebVitalsResponse.State state = switch (row.getStatus()) {
            case READY -> WebVitalsResponse.State.READY;
            case FAILED -> WebVitalsResponse.State.FAILED;
            case PENDING -> row.getCreatedAt().plus(properties.getStaleAfter()).isBefore(Instant.now())
                    ? WebVitalsResponse.State.FAILED
                    : WebVitalsResponse.State.PENDING;
        };
        String error = state == WebVitalsResponse.State.FAILED && row.getErrorMessage() == null
                ? "The measurement did not finish."
                : row.getErrorMessage();
        boolean ready = state == WebVitalsResponse.State.READY;
        boolean hasField = row.getFieldLcpMs() != null || row.getFieldCls() != null || row.getFieldInpMs() != null;

        return new WebVitalsResponse(
                state,
                row.getStrategy(),
                row.getUrl(),
                row.getPerformanceScore(),
                ready ? new WebVitalsResponse.Lab(row.getLabLcpMs(), row.getLabCls(), row.getLabTbtMs(),
                        row.getLabFcpMs(), row.getLabSpeedIndexMs()) : null,
                ready && hasField ? new WebVitalsResponse.Field(row.getFieldLcpMs(), row.getFieldCls(),
                        row.getFieldInpMs(), row.getFieldCategory()) : null,
                error,
                row.getMeasuredAt()
        );
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
