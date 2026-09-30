package com.seopulse.website.repository;

import com.seopulse.website.entity.AuditOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditOutboxRepository
        extends JpaRepository<AuditOutbox, Long> {

    /**
     * Locks a batch of unpublished events. SKIP LOCKED lets several
     * publishers run at once without publishing the same event twice.
     */
    @Query(value = """
            SELECT * FROM audit_outbox
            WHERE published = false
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AuditOutbox> lockUnpublishedBatch(@Param("limit") int limit);

    @Query("SELECT MIN(o.createdAt) FROM AuditOutbox o WHERE o.published = false")
    Optional<Instant> findOldestUnpublishedCreatedAt();

    @Modifying
    @Query("""
            DELETE FROM AuditOutbox o
            WHERE o.audit.id = :auditId
            """)
    int deleteByAuditId(
            @Param("auditId") Long auditId
    );
}
