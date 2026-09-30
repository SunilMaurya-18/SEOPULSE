package com.seopulse.website.repository;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    Page<Audit> findByWebsiteId(
            Long websiteId,
            Pageable pageable
    );

    Page<Audit> findByWebsiteIdAndStatus(
            Long websiteId,
            AuditStatus status,
            Pageable pageable
    );

    long countByWebsiteProjectId(Long projectId);

    long countByWebsiteProjectIdAndStatus(
            Long projectId,
            AuditStatus status
    );

    long countByWebsiteProjectIdAndStatusIn(
            Long projectId,
            List<AuditStatus> statuses
    );

    boolean existsByWebsiteIdAndStatusIn(
            Long websiteId,
            Iterable<AuditStatus> statuses
    );

    Optional<Audit> findByIdAndWebsiteProjectIdAndWebsiteProjectUserId(
            Long auditId,
            Long projectId,
            Long userId
    );

    Optional<Audit> findFirstByWebsiteIdOrderByCreatedAtDesc(
            Long websiteId
    );

    @Query("""
        SELECT a
        FROM Audit a
        JOIN FETCH a.website
        WHERE a.id = :auditId
    """)
    Optional<Audit> findByIdWithWebsite(
            @Param("auditId") Long auditId
    );
     List<Audit> findByWebsiteProjectId(Long projectId);
    @Modifying
    @Query("""
        DELETE FROM Audit a
        WHERE a.website.id = :websiteId
        """)
    int deleteByWebsiteId(@Param("websiteId") Long websiteId);
    @Query("""
        SELECT a.id
        FROM Audit a
        WHERE a.website.id = :websiteId
        """)
    List<Long> findIdsByWebsiteId(
            @Param("websiteId") Long websiteId
    );

    @Query("SELECT a.status FROM Audit a WHERE a.id = :auditId")
    Optional<AuditStatus> findStatusById(@Param("auditId") Long auditId);

    @Query("SELECT a.website.project.user.id FROM Audit a WHERE a.id = :auditId")
    Optional<Long> findOwnerIdById(@Param("auditId") Long auditId);

    // Status transitions are conditional updates so that concurrent
    // workers and user cancellation can never overwrite each other.

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Audit a
        SET a.status = :to, a.startedAt = :now, a.completedAt = NULL, a.errorMessage = NULL
        WHERE a.id = :auditId AND a.status = :from
        """)
    int claim(
            @Param("auditId") Long auditId,
            @Param("from") AuditStatus from,
            @Param("to") AuditStatus to,
            @Param("now") Instant now
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Audit a
        SET a.status = :to, a.pagesCrawled = :pagesCrawled, a.errorMessage = :message
        WHERE a.id = :auditId AND a.status = :from
        """)
    int completeCrawl(
            @Param("auditId") Long auditId,
            @Param("from") AuditStatus from,
            @Param("to") AuditStatus to,
            @Param("pagesCrawled") int pagesCrawled,
            @Param("message") String message
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Audit a
        SET a.status = :to, a.score = :score, a.pagesAnalyzed = :pagesAnalyzed, a.completedAt = :now
        WHERE a.id = :auditId AND a.status = :from
        """)
    int completeAnalysis(
            @Param("auditId") Long auditId,
            @Param("from") AuditStatus from,
            @Param("to") AuditStatus to,
            @Param("score") int score,
            @Param("pagesAnalyzed") int pagesAnalyzed,
            @Param("now") Instant now
    );

    /** Moves an active audit to a final status (FAILED or CANCELLED). */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Audit a
        SET a.status = :to, a.completedAt = :now, a.errorMessage = :message
        WHERE a.id = :auditId AND a.status IN :from
        """)
    int finish(
            @Param("auditId") Long auditId,
            @Param("from") Collection<AuditStatus> from,
            @Param("to") AuditStatus to,
            @Param("now") Instant now,
            @Param("message") String message
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Audit a
        SET a.status = :to, a.retryCount = :retryCount, a.errorMessage = :message,
            a.pagesCrawled = 0, a.pagesAnalyzed = 0, a.score = NULL
        WHERE a.id = :auditId AND a.status IN :from
        """)
    int requeue(
            @Param("auditId") Long auditId,
            @Param("from") Collection<AuditStatus> from,
            @Param("to") AuditStatus to,
            @Param("retryCount") int retryCount,
            @Param("message") String message
    );

    @Query("""
        SELECT a.id FROM Audit a
        WHERE a.status IN :statuses AND a.startedAt < :startedBefore
        """)
    List<Long> findStuckIds(
            @Param("statuses") Collection<AuditStatus> statuses,
            @Param("startedBefore") Instant startedBefore
    );
}