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

    List<AuditPage> findByAuditIdAndDepthOrderByIdAsc(Long auditId, Integer depth);

    @Modifying
    @Query("""
            DELETE FROM AuditPage p
            WHERE p.audit.id = :auditId
            """)
    int deleteByAuditId(@Param("auditId") Long auditId);

    /** Issues are removed with their pages by the database cascade. */
    @Modifying
    @Query(value = "DELETE FROM audit_pages WHERE audit_id IN (:auditIds)", nativeQuery = true)
    int deleteByAuditIds(@Param("auditIds") java.util.Collection<Long> auditIds);
}