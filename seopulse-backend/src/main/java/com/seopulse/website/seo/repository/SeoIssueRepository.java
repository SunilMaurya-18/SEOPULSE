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

    @Query("""
            SELECT s.auditPage.id, s.ruleCode
            FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId
            """)
    List<Object[]> findPageRulePairsByAuditId(@Param("auditId") Long auditId);

    default List<String> findPageRuleKeysByAuditId(Long auditId) {
        return findPageRulePairsByAuditId(auditId).stream()
                .map(row -> row[0] + "|" + row[1])
                .toList();
    }

    /** Everything scoring needs, for all pages of an audit, in one query. */
    @Query("""
            SELECT new com.seopulse.website.seo.model.IssueRow(
                s.auditPage.id, s.ruleCode, s.severity, s.category)
            FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId
            """)
    List<com.seopulse.website.seo.model.IssueRow> findRowsByAuditId(@Param("auditId") Long auditId);

    @Query("""
            SELECT new com.seopulse.website.seo.model.IssueSnapshot(
                s.fingerprint, s.ruleCode, s.severity, s.category, s.auditPage.url, s.message)
            FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId
            """)
    List<com.seopulse.website.seo.model.IssueSnapshot> findSnapshotsByAuditId(@Param("auditId") Long auditId);

    @Query("""
            SELECT s.fingerprint
            FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId AND s.fingerprint IS NOT NULL
            """)
    List<String> findFingerprintsByAuditId(@Param("auditId") Long auditId);

    @Modifying
    @Query("""
            DELETE FROM SeoIssue s
            WHERE s.auditPage.audit.id = :auditId
            """)
    int deleteByAuditId(
            @Param("auditId") Long auditId
    );
}