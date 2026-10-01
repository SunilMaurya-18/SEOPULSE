package com.seopulse.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Delivers queued alerts. Rows are claimed with a short lease in one
 * transaction and delivered outside it, so slow receivers never hold
 * database locks.
 */
@Component
@Slf4j
public class AlertDispatcher {

    static final int BATCH_SIZE = 20;
    static final int MAX_ATTEMPTS = 6;
    static final Duration LEASE = Duration.ofMinutes(5);
    static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1),
            Duration.ofHours(6)
    );

    private final AlertOutboxRepository outboxRepository;
    private final AlertRuleRepository ruleRepository;
    private final AlertDelivery delivery;
    private final TransactionTemplate transactionTemplate;

    public AlertDispatcher(
            AlertOutboxRepository outboxRepository,
            AlertRuleRepository ruleRepository,
            AlertDelivery delivery,
            PlatformTransactionManager transactionManager
    ) {
        this.outboxRepository = outboxRepository;
        this.ruleRepository = ruleRepository;
        this.delivery = delivery;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int dispatchPending() {

        List<AlertOutbox> claimed = transactionTemplate.execute(status -> {
            List<AlertOutbox> rows = outboxRepository.lockDispatchable(MAX_ATTEMPTS, BATCH_SIZE);
            Instant leaseUntil = Instant.now().plus(LEASE);
            rows.forEach(row -> row.setNextAttemptAt(leaseUntil));
            return rows;
        });

        int delivered = 0;
        for (AlertOutbox alert : claimed == null ? List.<AlertOutbox>of() : claimed) {
            String error = send(alert);
            transactionTemplate.executeWithoutResult(status -> record(alert.getId(), error));
            if (error == null) {
                delivered++;
            }
        }
        return delivered;
    }

    private String send(AlertOutbox alert) {
        try {
            String secret = alert.getAlertRuleId() == null ? null : ruleRepository.findById(alert.getAlertRuleId())
                    .map(AlertRule::getSigningSecret)
                    .orElse(null);
            delivery.deliver(
                    alert.getOrganizationId(),
                    alert.getChannel(),
                    alert.getTarget(),
                    secret,
                    alert.getEventType(),
                    "alert_" + alert.getId(),
                    alert.getSubject(),
                    alert.getPayload()
            );
            return null;
        } catch (RuntimeException ex) {
            return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
    }

    private void record(Long id, String error) {
        outboxRepository.findById(id).ifPresent(alert -> {
            Instant now = Instant.now();
            if (error == null) {
                alert.setDelivered(true);
                alert.setDeliveredAt(now);
                alert.setLastError(null);
                return;
            }
            int attempts = alert.getAttempts() + 1;
            alert.setAttempts(attempts);
            alert.setLastError(AlertService.truncate(error, 500));
            alert.setNextAttemptAt(now.plus(backoff(attempts)));
            log.warn("Alert delivery failed: id={}, channel={}, attempts={}, error={}",
                    id, alert.getChannel(), attempts, alert.getLastError());
        });
    }

    static Duration backoff(int attempts) {
        return BACKOFF.get(Math.min(Math.max(attempts, 1), BACKOFF.size()) - 1);
    }
}
