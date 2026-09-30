package com.seopulse.website.repository;

import com.seopulse.website.entity.AuditPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditPageRepository
        extends JpaRepository<AuditPage, Long> {

    List<AuditPage> findByAuditId(Long auditId);

    Page<AuditPage> findByAuditId(
            Long auditId,
            Pageable pageable
    );

    boolean existsByAuditIdAndUrl(
            Long auditId,
            String url
    );

    long countByAuditId(Long auditId);

    @Modifying
    @Query("""
            DELETE FROM AuditPage p
            WHERE p.audit.id = :auditId
            """)
    int deleteByAuditId(@Param("auditId") Long auditId);
}