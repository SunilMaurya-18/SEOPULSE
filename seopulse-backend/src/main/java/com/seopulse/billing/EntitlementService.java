package com.seopulse.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.billing.entity.Plan;
import com.seopulse.billing.entity.Subscription;
import com.seopulse.billing.entity.SubscriptionStatus;
import com.seopulse.billing.entity.UsageCounter;
import com.seopulse.billing.repository.PlanRepository;
import com.seopulse.billing.repository.SubscriptionRepository;
import com.seopulse.billing.repository.UsageCounterRepository;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.repository.InvitationRepository;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.WebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    public static final String AUDITS = "AUDITS";

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UsageCounterRepository usageCounterRepository;
    private final WebsiteRepository websiteRepository;
    private final OrganizationMemberRepository memberRepository;
    private final InvitationRepository invitationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PlanLimits limitsFor(Long organizationId) {
        return PlanLimits.parse(effectivePlan(organizationId).getLimits(), objectMapper);
    }

    @Transactional(readOnly = true)
    public int pagesPerAudit(Long organizationId) {
        return limitsFor(organizationId).pagesPerAudit();
    }

    @Transactional
    public void checkWebsiteCapacity(Long organizationId) {
        PlanLimits limits = limitsFor(organizationId);
        int used = (int) websiteRepository.countByProjectOrganizationIdAndStatusNot(
                organizationId,
                WebsiteStatus.LOCKED
        );
        if (used >= limits.websites()) {
            throw exceeded("websites", limits.websites(), used, upgradeTarget(organizationId));
        }
    }

    @Transactional
    public void checkMemberCapacity(Long organizationId) {
        PlanLimits limits = limitsFor(organizationId);
        int used = (int) memberRepository.countByOrganizationId(organizationId)
                + (int) invitationRepository.countByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNull(organizationId);
        if (used >= limits.members()) {
            throw exceeded("members", limits.members(), used, upgradeTarget(organizationId));
        }
    }

    @Transactional
    public void consumeAudit(Long organizationId) {
        PlanLimits limits = limitsFor(organizationId);
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        ensureCounter(organizationId, period);
        int updated = usageCounterRepository.consume(organizationId, AUDITS, period, 1, limits.auditsPerMonth());
        if (updated == 0) {
            int used = usageCounterRepository
                    .findByOrganizationIdAndMeterAndPeriodStart(organizationId, AUDITS, period)
                    .map(UsageCounter::getUsed)
                    .orElse(limits.auditsPerMonth());
            throw exceeded(AUDITS, limits.auditsPerMonth(), used, upgradeTarget(organizationId));
        }
    }

    @Transactional(readOnly = true)
    public int auditsUsed(Long organizationId) {
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        return usageCounterRepository
                .findByOrganizationIdAndMeterAndPeriodStart(organizationId, AUDITS, period)
                .map(UsageCounter::getUsed)
                .orElse(0);
    }

    @Transactional
    public void lockExcessWebsites(Long organizationId) {
        PlanLimits limits = limitsFor(organizationId);
        var sites = websiteRepository.findByProjectOrganizationIdOrderByCreatedAtAsc(organizationId);
        int kept = 0;
        for (var site : sites) {
            if (site.getStatus() == WebsiteStatus.LOCKED) {
                continue;
            }
            kept++;
            if (kept > limits.websites()) {
                site.setStatus(WebsiteStatus.LOCKED);
            }
        }
    }

    private void ensureCounter(Long organizationId, LocalDate period) {
        if (usageCounterRepository
                .findByOrganizationIdAndMeterAndPeriodStart(organizationId, AUDITS, period)
                .isPresent()) {
            return;
        }
        try {
            usageCounterRepository.saveAndFlush(UsageCounter.builder()
                    .organizationId(organizationId)
                    .meter(AUDITS)
                    .periodStart(period)
                    .used(0)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Another request inserted the row first.
        }
    }

    private Plan effectivePlan(Long organizationId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        SubscriptionStatus status = subscription.getStatus();
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID) {
            return freePlan();
        }
        if (status == SubscriptionStatus.PAST_DUE) {
            Instant grace = subscription.getGraceUntil();
            if (grace != null && grace.isAfter(Instant.now())) {
                return subscription.getPlan();
            }
            return freePlan();
        }
        return subscription.getPlan();
    }

    private String upgradeTarget(Long organizationId) {
        String code = effectivePlan(organizationId).getCode();
        return "FREE".equals(code) ? "PRO" : "AGENCY";
    }

    private Plan freePlan() {
        return planRepository.findByCode("FREE")
                .orElseThrow(() -> new ResourceNotFoundException("Free plan is not configured"));
    }

    private static PlanLimitExceededException exceeded(String meter, int limit, int used, String upgradeTo) {
        return new PlanLimitExceededException(meter, limit, used, upgradeTo);
    }
}
