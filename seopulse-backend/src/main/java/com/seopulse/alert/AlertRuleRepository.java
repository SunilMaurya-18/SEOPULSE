package com.seopulse.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByOrganizationIdOrderByIdAsc(Long organizationId);

    Optional<AlertRule> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationId(Long organizationId);

    @Query("""
        SELECT r FROM AlertRule r
        WHERE r.organizationId = :organizationId
          AND r.enabled = true
          AND (r.websiteId IS NULL OR r.websiteId = :websiteId)
        ORDER BY r.id
        """)
    List<AlertRule> findApplicable(
            @Param("organizationId") Long organizationId,
            @Param("websiteId") Long websiteId
    );
}
