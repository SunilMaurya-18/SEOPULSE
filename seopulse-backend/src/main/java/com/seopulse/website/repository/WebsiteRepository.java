package com.seopulse.website.repository;

import com.seopulse.website.entity.Website;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebsiteRepository extends JpaRepository<Website, Long> {

    Optional<Website> findByIdAndProjectIdAndProjectUserId(
            Long websiteId,
            Long projectId,
            Long userId
    );

    boolean existsByProjectIdAndUrl(
            Long projectId,
            String url
    );

    Page<Website> findByProjectId(
            Long projectId,
            Pageable pageable
    );

    long countByProjectId(Long projectId);

    @Query("""
            SELECT w.id
            FROM Website w
            WHERE w.project.id = :projectId
            """)
    List<Long> findIdsByProjectId(
            @Param("projectId") Long projectId
    );

    @Modifying
    @Query("""
            DELETE FROM Website w
            WHERE w.project.id = :projectId
            """)
    int deleteByProjectId(
            @Param("projectId") Long projectId
    );
}