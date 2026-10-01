package com.seopulse.organization.repository;

import com.seopulse.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlug(String slug);

    /** Returns 1 for the single caller that gets to create the default alert rules. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Organization o SET o.alertDefaultsApplied = true WHERE o.id = :id AND o.alertDefaultsApplied = false")
    int claimAlertDefaults(@Param("id") Long id);

    @Query("SELECT o.id FROM Organization o ORDER BY o.id")
    List<Long> findAllIds();
}
