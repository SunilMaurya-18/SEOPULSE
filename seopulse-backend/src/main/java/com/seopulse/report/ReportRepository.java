package com.seopulse.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findTop10ByAuditIdOrderByCreatedAtDesc(Long auditId);

    Optional<Report> findByIdAndAuditId(Long id, Long auditId);

    Optional<Report> findFirstByAuditIdAndStatusAndWatermarkOrderByCreatedAtDesc(Long auditId, ReportStatus status, boolean watermark);

    boolean existsByAuditIdAndStatusIn(Long auditId, Collection<ReportStatus> statuses);

    List<Report> findByAuditIdAndStatusIn(Long auditId, Collection<ReportStatus> statuses);

    @Query(value = """
        SELECT * FROM reports
        WHERE status = 'PENDING'
        ORDER BY created_at
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Report> lockNextPending();

    /** Returns reports stuck in GENERATING (worker crashed mid-render) to the queue. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Report r SET r.status = com.seopulse.report.ReportStatus.PENDING
        WHERE r.status = com.seopulse.report.ReportStatus.GENERATING AND r.createdAt < :before
        """)
    int requeueStale(@Param("before") Instant before);

    @Query("SELECT r FROM Report r WHERE r.auditId IN :auditIds AND r.storageKey IS NOT NULL")
    List<Report> findStoredByAuditIds(@Param("auditIds") Collection<Long> auditIds);

    @Query("SELECT r.storageKey FROM Report r WHERE r.organizationId = :organizationId AND r.storageKey IS NOT NULL")
    List<String> findStorageKeysByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT COUNT(r) > 0 FROM Report r, Audit a WHERE a.id = r.auditId AND a.website.project.id = :projectId")
    boolean existsForProject(@Param("projectId") Long projectId);
}
