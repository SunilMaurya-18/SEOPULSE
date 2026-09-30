package com.seopulse.website.repository;

import com.seopulse.website.entity.AuditOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditOutboxRepository
        extends JpaRepository<AuditOutbox, Long> {

    Page<AuditOutbox> findByPublishedFalseOrderByCreatedAtAsc(
            Pageable pageable
    );

    @Modifying
    @Query("""
            DELETE FROM AuditOutbox o
            WHERE o.audit.id = :auditId
            """)
    int deleteByAuditId(
            @Param("auditId") Long auditId
    );
}