package com.seopulse.alert;

import java.time.Instant;

public record AlertDeliveryResponse(
        Long id,
        Long ruleId,
        Long auditId,
        String eventType,
        AlertChannel channel,
        String subject,
        boolean delivered,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant deliveredAt
) {

    static AlertDeliveryResponse from(AlertOutbox outbox) {
        return new AlertDeliveryResponse(
                outbox.getId(),
                outbox.getAlertRuleId(),
                outbox.getAuditId(),
                outbox.getEventType(),
                outbox.getChannel(),
                outbox.getSubject(),
                outbox.isDelivered(),
                outbox.getAttempts(),
                outbox.getLastError(),
                outbox.getCreatedAt(),
                outbox.getDeliveredAt()
        );
    }
}
