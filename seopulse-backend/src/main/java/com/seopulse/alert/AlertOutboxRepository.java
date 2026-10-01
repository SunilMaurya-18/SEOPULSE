package com.seopulse.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AlertOutboxRepository extends JpaRepository<AlertOutbox, Long> {

    boolean existsByAlertRuleIdAndAuditIdAndEventType(Long alertRuleId, Long auditId, String eventType);

    List<AlertOutbox> findTop50ByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    @Query(value = """
        SELECT * FROM alert_outbox
        WHERE NOT delivered
          AND attempts < :maxAttempts
          AND next_attempt_at <= NOW()
        ORDER BY next_attempt_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<AlertOutbox> lockDispatchable(@Param("maxAttempts") int maxAttempts, @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM AlertOutbox a WHERE a.delivered = true AND a.deliveredAt < :before")
    int deleteDeliveredBefore(@Param("before") Instant before);
}
