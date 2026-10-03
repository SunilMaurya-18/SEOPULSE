package com.seopulse.billing;

import com.seopulse.billing.entity.Plan;
import com.seopulse.billing.razorpay.RazorpayBillingService;
import com.seopulse.billing.entity.StripeEventRecord;
import com.seopulse.billing.entity.Subscription;
import com.seopulse.billing.entity.SubscriptionStatus;
import com.seopulse.billing.repository.PlanRepository;
import com.seopulse.billing.repository.StripeEventRepository;
import com.seopulse.billing.repository.SubscriptionRepository;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.Organization;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.organization.service.OrganizationAccessService;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.WebsiteRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final BillingProperties properties;
    private final OrganizationAccessService accessService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeEventRepository stripeEventRepository;
    private final EntitlementService entitlementService;
    private final EmailOutboxService emailOutboxService;
    private final WebsiteRepository websiteRepository;
    private final RazorpayBillingService razorpayBillingService;

    @Transactional(readOnly = true)
    public BillingSnapshot snapshot(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        PlanLimits limits = entitlementService.limitsFor(organizationId);
        int websites = (int) websiteRepository.countByProjectOrganizationIdAndStatusNot(
                organizationId,
                WebsiteStatus.LOCKED
        );
        return new BillingSnapshot(
                subscription.getPlan().getCode(),
                subscription.getPlan().getName(),
                subscription.getStatus().name(),
                subscription.getCurrentPeriodEnd(),
                subscription.getTrialEnd(),
                subscription.isCancelAtPeriodEnd(),
                limits,
                entitlementService.auditsUsed(organizationId),
                websites,
                subscription.getBillingProvider(),
                properties.stripeEnabled(),
                razorpayBillingService.isEnabled()
        );
    }

    public String checkout(Long organizationId, Long userId, String planCode, String interval) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        requireStripe();
        subscriptionRepository.findByOrganizationId(organizationId)
                .filter(current -> RazorpayBillingService.PROVIDER.equals(current.getBillingProvider())
                        && current.getStatus() != SubscriptionStatus.CANCELED)
                .ifPresent(current -> {
                    throw new InvalidStateException("This workspace pays through Razorpay. Cancel that subscription first.");
                });
        String priceId = properties.priceId(planCode, interval);
        if (priceId == null || priceId.isBlank()) {
            throw new InvalidStateException("Stripe price is not configured for " + planCode);
        }
        Organization organization = organization(organizationId);
        Stripe.apiKey = properties.getStripeSecretKey();
        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(properties.getSuccessUrl())
                    .setCancelUrl(properties.getCancelUrl())
                    .setClientReferenceId(String.valueOf(organizationId))
                    .putMetadata("organizationId", String.valueOf(organizationId))
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("organizationId", String.valueOf(organizationId))
                            .build())
                    .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build());
            if (organization.getStripeCustomerId() != null) {
                builder.setCustomer(organization.getStripeCustomerId());
            }
            Session session = Session.create(builder.build());
            return session.getUrl();
        } catch (StripeException ex) {
            throw new InvalidStateException("Unable to start checkout");
        }
    }

    public String portal(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        requireStripe();
        Organization organization = organization(organizationId);
        if (organization.getStripeCustomerId() == null) {
            throw new InvalidStateException("No billing account exists yet");
        }
        Stripe.apiKey = properties.getStripeSecretKey();
        try {
            var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(organization.getStripeCustomerId())
                    .setReturnUrl(properties.getSuccessUrl())
                    .build();
            return com.stripe.model.billingportal.Session.create(params).getUrl();
        } catch (StripeException ex) {
            throw new InvalidStateException("Unable to open the billing portal");
        }
    }

    /** Ends a paid subscription immediately, so a deleted workspace is never billed again. */
    public void cancelBeforeDeletion(Long organizationId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        if (subscription == null || subscription.getStatus() == SubscriptionStatus.CANCELED) {
            return;
        }
        if (RazorpayBillingService.PROVIDER.equals(subscription.getBillingProvider())
                && subscription.getRazorpaySubscriptionId() != null) {
            razorpayBillingService.cancelImmediately(subscription);
            return;
        }
        if (subscription.getStripeSubscriptionId() == null) {
            return;
        }
        if (!properties.stripeEnabled()) {
            log.warn("Stripe is not configured; subscription left as is: organizationId={}", organizationId);
            return;
        }
        Stripe.apiKey = properties.getStripeSecretKey();
        try {
            com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId()).cancel();
        } catch (StripeException ex) {
            throw new InvalidStateException(
                    "Could not cancel the subscription for this workspace. Cancel it from billing, then try again.");
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!properties.stripeEnabled() || properties.getStripeWebhookSecret() == null
                || properties.getStripeWebhookSecret().isBlank()) {
            throw new InvalidStateException("Stripe is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.getStripeWebhookSecret());
        } catch (SignatureVerificationException ex) {
            throw new InvalidStateException("Invalid Stripe signature");
        }
        if (stripeEventRepository.existsById(event.getId())) {
            return;
        }
        apply(event);
        stripeEventRepository.save(StripeEventRecord.builder()
                .id(event.getId())
                .type(event.getType())
                .processedAt(Instant.now())
                .build());
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void reconcile() {
        for (Subscription subscription : subscriptionRepository.findByStatusNot(SubscriptionStatus.CANCELED)) {
            if ("FREE".equals(subscription.getPlan().getCode())) {
                continue;
            }
            if (RazorpayBillingService.PROVIDER.equals(subscription.getBillingProvider())) {
                if (razorpayBillingService.isEnabled()) {
                    try {
                        razorpayBillingService.refresh(subscription.getRazorpaySubscriptionId());
                    } catch (RuntimeException ex) {
                        log.warn("Razorpay reconciliation skipped: subscriptionId={}", subscription.getRazorpaySubscriptionId());
                    }
                }
                continue;
            }
            if (subscription.getStripeSubscriptionId() == null || !properties.stripeEnabled()) {
                continue;
            }
            Stripe.apiKey = properties.getStripeSecretKey();
            try {
                applyStripeSubscription(com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId()));
            } catch (StripeException ex) {
                log.warn("Stripe reconciliation skipped: subscriptionId={}", subscription.getStripeSubscriptionId());
            }
        }
    }

    private void apply(Event event) {
        Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isEmpty()) {
            return;
        }
        StripeObject data = object.get();
        try {
            Stripe.apiKey = properties.getStripeSecretKey();
            if (data instanceof Session session && session.getSubscription() != null) {
                applyStripeSubscription(com.stripe.model.Subscription.retrieve(session.getSubscription()));
            } else if (data instanceof com.stripe.model.Subscription subscription) {
                applyStripeSubscription(com.stripe.model.Subscription.retrieve(subscription.getId()));
            } else if (data instanceof Invoice invoice && invoice.getSubscription() != null) {
                applyStripeSubscription(com.stripe.model.Subscription.retrieve(invoice.getSubscription()));
                if ("invoice.payment_failed".equals(event.getType())) {
                    notifyOwners(organizationId(invoice.getSubscription()), "Payment failed",
                            "Update the card on file so SEOPulse can keep your plan active.");
                }
            }
        } catch (StripeException ex) {
            throw new InvalidStateException("Unable to refresh the Stripe subscription");
        }
    }

    private void applyStripeSubscription(com.stripe.model.Subscription stripeSubscription) {
        String orgValue = stripeSubscription.getMetadata() == null
                ? null
                : stripeSubscription.getMetadata().get("organizationId");
        if (orgValue == null) {
            return;
        }
        Long organizationId = Long.valueOf(orgValue);
        Organization organization = organization(organizationId);
        if (stripeSubscription.getCustomer() != null) {
            organization.setStripeCustomerId(stripeSubscription.getCustomer());
        }
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        SubscriptionStatus status = mapStatus(stripeSubscription.getStatus());
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(status);
        subscription.setBillingProvider(status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID
                ? null
                : "STRIPE");
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
        if (stripeSubscription.getTrialEnd() != null) {
            subscription.setTrialEnd(Instant.ofEpochSecond(stripeSubscription.getTrialEnd()));
        }
        if (status == SubscriptionStatus.PAST_DUE && subscription.getGraceUntil() == null) {
            subscription.setGraceUntil(Instant.now().plus(7, ChronoUnit.DAYS));
        }
        if (status == SubscriptionStatus.ACTIVE) {
            subscription.setGraceUntil(null);
        }
        String priceId = stripeSubscription.getItems().getData().isEmpty()
                ? null
                : stripeSubscription.getItems().getData().get(0).getPrice().getId();
        Plan plan = planForPrice(priceId).orElse(subscription.getPlan());
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID) {
            plan = planRepository.findByCode("FREE").orElse(plan);
            notifyOwners(organizationId, "Subscription ended",
                    "SEOPulse moved this workspace to the Free plan. Extra websites are locked, not deleted.");
        }
        subscription.setPlan(plan);
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID) {
            entitlementService.lockExcessWebsites(organizationId);
        }
    }

    private Optional<Plan> planForPrice(String priceId) {
        if (priceId == null) {
            return Optional.empty();
        }
        return planRepository.findAll().stream()
                .filter(plan -> priceId.equals(plan.getStripeMonthlyPriceId()) || priceId.equals(plan.getStripeYearlyPriceId()))
                .findFirst();
    }

    private void notifyOwners(Long organizationId, String subject, String body) {
        memberRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .filter(member -> member.getRole() == OrganizationRole.OWNER)
                .forEach(member -> emailOutboxService.enqueue(
                        member.getUser().getEmail(),
                        subject,
                        body,
                        EmailTemplates.html(subject, body, null, null)
                ));
    }

    private Long organizationId(String stripeSubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(subscription -> subscription.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private Organization organization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private void requireStripe() {
        if (!properties.stripeEnabled()) {
            throw new InvalidStateException("Stripe is not configured");
        }
    }

    private static SubscriptionStatus mapStatus(String status) {
        return switch (status) {
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    public record BillingSnapshot(
            String planCode,
            String planName,
            String status,
            Instant currentPeriodEnd,
            Instant trialEnd,
            boolean cancelAtPeriodEnd,
            PlanLimits limits,
            int auditsUsed,
            int websitesUsed,
            /** "STRIPE", "RAZORPAY", or null on the free plan. */
            String provider,
            boolean stripeAvailable,
            boolean razorpayAvailable
    ) {
    }
}
