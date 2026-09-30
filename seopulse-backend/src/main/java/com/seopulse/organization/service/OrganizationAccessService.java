package com.seopulse.organization.service;

import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationAccessService {

    private final OrganizationMemberRepository memberRepository;

    public OrganizationMember requireRole(Long organizationId, Long userId, OrganizationRole minimum) {
        OrganizationMember member = memberRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (!member.getRole().atLeast(minimum)) {
            throw new AccessDeniedException("You do not have permission for this action");
        }
        return member;
    }
}
