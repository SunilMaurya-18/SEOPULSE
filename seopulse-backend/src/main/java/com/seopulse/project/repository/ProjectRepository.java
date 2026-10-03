package com.seopulse.project.repository;

import com.seopulse.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserId(
            Long projectId,
            Long userId
    );

    @Query("""
            select p from Project p
            where p.id = :projectId
              and (
                p.user.id = :userId
                or exists (
                  select m.id from OrganizationMember m
                  where m.organization = p.organization
                    and m.user.id = :userId
                )
              )
            """)
    Optional<Project> findAccessible(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

    Page<Project> findByUserId(
            Long userId,
            Pageable pageable
    );

    long countByUserId(Long userId);

    List<Project> findByOrganizationIdOrderByIdAsc(Long organizationId);

    @Query("SELECT DISTINCT p.organization.id FROM Project p WHERE p.user.id = :userId")
    List<Long> findOrganizationIdsByUserId(@Param("userId") Long userId);

    /** Websites, audits and everything below them go with the project through ON DELETE CASCADE. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Project p WHERE p.organization.id = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") Long organizationId);

    /** projects.user_id cascades on delete, so a leaving creator hands their projects to a teammate first. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE projects SET user_id = :newUserId
            WHERE user_id = :userId AND organization_id = :organizationId
            """, nativeQuery = true)
    int reassignCreator(
            @Param("userId") Long userId,
            @Param("newUserId") Long newUserId,
            @Param("organizationId") Long organizationId
    );
}
