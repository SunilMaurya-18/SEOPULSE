package com.seopulse.project.repository;

import com.seopulse.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
