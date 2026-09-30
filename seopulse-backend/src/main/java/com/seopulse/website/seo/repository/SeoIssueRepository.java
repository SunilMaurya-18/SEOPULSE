package com.seopulse.website.seo.repository;

import com.seopulse.website.seo.entity.SeoIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeoIssueRepository extends JpaRepository<SeoIssue, Long> {

    List<SeoIssue> findByAuditPageId(Long auditPageId);

    Page<SeoIssue> findByAuditPageAuditId(
            Long auditId,
            Pageable pageable
    );

    Page<SeoIssue> findByAuditPageAuditIdAndSeverityIgnoreCase(
            Long auditId,
            String severity,
            Pageable pageable
    );

    Page<SeoIssue> findByAuditPageAuditIdAndRuleCode(
            Long auditId,
            String ruleCode,
            Pageable pageable
    );

    Page<SeoIssue> findByAuditPageAuditIdAndSeverityIgnoreCaseAndRuleCode(
            Long auditId,
            String severity,
            String ruleCode,
            Pageable pageable
    );

    long countByAuditPageId(Long auditPageId);

    long countByAuditPageAuditId(Long auditId);

    long countByAuditPageAuditIdAndSeverityIgnoreCase(
            Long auditId,
            String severity
    );

    void deleteByAuditPageId(Long auditPageId);

    @Modifying
    @Query("""
            DELETE FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId
            """)
    int deleteByAuditId(
            @Param("auditId") Long auditId
    );
}