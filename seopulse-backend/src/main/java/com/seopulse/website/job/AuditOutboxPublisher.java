package com.seopulse.website.job;

import com.seopulse.website.entity.AuditOutbox;
import com.seopulse.website.repository.AuditOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditOutboxPublisher {

    private final AuditOutboxRepository auditOutboxRepository;
    private final AuditQueue auditQueue;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {

        List<AuditOutbox> events =
                auditOutboxRepository
                        .findByPublishedFalseOrderByCreatedAtAsc(
                                PageRequest.of(0, 20)
                        )
                        .getContent();

        for (AuditOutbox event : events) {

            try {

                auditQueue.enqueue(event.getAudit().getId());

                event.setPublished(true);
                event.setPublishedAt(Instant.now());

                auditOutboxRepository.save(event);

                log.debug(
                        "Published audit outbox event: eventId={}, auditId={}",
                        event.getId(),
                        event.getAudit().getId()
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to publish audit outbox event: eventId={}",
                        event.getId(),
                        ex
                );
            }
        }
    }
}