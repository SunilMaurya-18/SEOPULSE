package com.seopulse.website.job;

import com.seopulse.website.entity.AuditOutbox;
import com.seopulse.website.repository.AuditOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Moves committed audit-created events from the outbox table to the Redis
 * stream. Rows are locked with SKIP LOCKED, so several workers can publish
 * concurrently; a crash between XADD and commit can publish an event
 * twice, which the worker's atomic claim makes harmless.
 */
@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class AuditOutboxPublisher {

    private final AuditOutboxRepository auditOutboxRepository;
    private final AuditQueue auditQueue;
    private final WorkerProperties properties;

    @Scheduled(fixedDelayString = "${seopulse.worker.outbox-publish-interval-ms:2000}")
    @Transactional
    public void publishPendingEvents() {

        List<AuditOutbox> events = auditOutboxRepository.lockUnpublishedBatch(properties.getOutboxBatchSize());

        for (AuditOutbox event : events) {

            Long auditId = event.getAudit().getId();

            try {
                auditQueue.enqueue(auditId);

                event.setPublished(true);
                event.setPublishedAt(Instant.now());

                log.debug("Published audit outbox event: eventId={}, auditId={}", event.getId(), auditId);
            } catch (RuntimeException ex) {
                log.error("Failed to publish audit outbox event: eventId={}", event.getId(), ex);
                return;
            }
        }
    }
}
