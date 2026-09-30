package com.seopulse.project.repository;

import com.seopulse.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserId(
            Long projectId,
            Long userId
    );

    Page<Project> findByUserId(
            Long userId,
            Pageable pageable
    );

    long countByUserId(Long userId);
}
