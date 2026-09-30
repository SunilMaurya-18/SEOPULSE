package com.seopulse.organization.repository;

import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    @EntityGraph(attributePaths = "organization")
    List<OrganizationMember> findByUserIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<OrganizationMember> findByOrganizationIdOrderByIdAsc(Long organizationId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndRole(Long organizationId, OrganizationRole role);
}
