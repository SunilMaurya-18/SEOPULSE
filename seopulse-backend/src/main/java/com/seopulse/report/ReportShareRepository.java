package com.seopulse.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportShareRepository extends JpaRepository<ReportShare, Long> {

    Optional<ReportShare> findByTokenHash(String tokenHash);

    List<ReportShare> findByAuditIdOrderByCreatedAtDesc(Long auditId);

    Optional<ReportShare> findByIdAndAuditId(Long id, Long auditId);

    @Query("SELECT COUNT(s) > 0 FROM ReportShare s, Audit a WHERE a.id = s.auditId AND a.website.project.id = :projectId")
    boolean existsForProject(@Param("projectId") Long projectId);

    @Transactional
    @Modifying
    @Query("UPDATE ReportShare s SET s.viewCount = s.viewCount + 1, s.lastViewedAt = :now WHERE s.id = :id")
    int recordView(@Param("id") Long id, @Param("now") Instant now);

    /** Deletes links that expired or were revoked before {@code before}. */
    @Transactional
    @Modifying
    @Query("DELETE FROM ReportShare s WHERE s.expiresAt < :before OR s.revokedAt < :before")
    int deleteInactiveBefore(@Param("before") Instant before);
}
