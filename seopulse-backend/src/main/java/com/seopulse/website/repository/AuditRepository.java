package com.seopulse.website.repository;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}