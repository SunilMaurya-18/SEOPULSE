package com.seopulse.organization.service;

import com.seopulse.billing.entity.Plan;
import com.seopulse.billing.entity.Subscription;
import com.seopulse.billing.entity.SubscriptionStatus;
import com.seopulse.billing.repository.PlanRepository;
import com.seopulse.billing.repository.SubscriptionRepository;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.entity.Organization;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrganizationProvisioningService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Organization ensureFor(User user) {
        return memberRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .filter(member -> member.getRole() == OrganizationRole.OWNER)
                .map(OrganizationMember::getOrganization)
                .findFirst()
                .orElseGet(() -> createPersonal(user));
    }

    private Organization createPersonal(User user) {
        String slug = "user-" + user.getId();
        Organization organization = organizationRepository.findBySlug(slug).orElseGet(() ->
                organizationRepository.save(Organization.builder()
                        .name(displayName(user))
                        .slug(slug)
                        .build()));

        memberRepository.save(OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build());

        attachFreePlan(organization);
        return organization;
    }

    @Transactional
    public void attachFreePlan(Organization organization) {
        if (subscriptionRepository.findByOrganizationId(organization.getId()).isPresent()) {
            return;
        }
        Plan free = planRepository.findByCode("FREE")
                .orElseThrow(() -> new ResourceNotFoundException("Free plan is not configured"));
        subscriptionRepository.save(Subscription.builder()
                .organization(organization)
                .plan(free)
                .status(SubscriptionStatus.ACTIVE)
                .build());
    }

    private static String displayName(User user) {
        String name = user.getName() == null ? "" : user.getName().trim();
        if (name.isEmpty()) {
            return "Personal workspace";
        }
        return name.substring(0, Math.min(name.length(), 120)) + "'s workspace";
    }

    public static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "workspace" : slug;
    }
}
