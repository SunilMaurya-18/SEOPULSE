package com.seopulse.organization.repository;

import com.seopulse.organization.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNull(Long organizationId);

    Optional<Invitation> findByTokenHash(String tokenHash);

    long countByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNull(Long organizationId);
}
