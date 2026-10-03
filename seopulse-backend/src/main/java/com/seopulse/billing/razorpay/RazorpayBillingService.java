package com.seopulse.billing.razorpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.billing.EntitlementService;
import com.seopulse.billing.entity.Plan;
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
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * INR subscriptions through Razorpay. The flow: create a subscription here,
 * open Razorpay Checkout in the browser, verify the payment signature, and
 * keep the plan in sync from webhooks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayBillingService {

    public static final String PROVIDER = "RAZORPAY";

    public record CheckoutSession(String keyId, String subscriptionId, String organizationName, String name, String email) {
    }

    private final RazorpayProperties properties;
    private final RazorpayClient client;
    private final OrganizationAccessService accessService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeEventRepository eventRepository;
    private final EntitlementService entitlementService;
    private final EmailOutboxService emailOutboxService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return properties.enabled();
    }

    public CheckoutSession startCheckout(Long organizationId, Long userId, String planCode, String interval) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        requireEnabled();
        String planId = properties.planId(planCode, interval);
        if (planId.isBlank()) {
            throw new InvalidStateException("Razorpay plan is not configured for " + planCode);
        }
        Subscription current = subscription(organizationId);
        if (isPaid(current)) {
            throw new InvalidStateException("This workspace already has a paid plan. Cancel it first, then choose a new one.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        boolean yearly = "year".equalsIgnoreCase(interval);
        JsonNode created = client.createSubscription(planId, yearly ? 10 : 120,
                Map.of("organizationId", String.valueOf(organizationId)));

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("Razorpay subscription created: organizationId={}, subscriptionId={}", organizationId, created.path("id").asText());
        return new CheckoutSession(properties.getKeyId(), created.path("id").asText(), organization.getName(),
                user.getName(), user.getEmail());
    }

    /** Called by the browser after Checkout succeeds, so the plan updates without waiting for the webhook. */
    @Transactional
    public void confirmPayment(Long organizationId, Long userId, String paymentId, String subscriptionId, String signature) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        requireEnabled();
        if (!signatureMatches(paymentId + "|" + subscriptionId, signature, properties.getKeySecret())) {
            throw new IllegalArgumentException("Payment could not be verified.");
        }
        JsonNode remote = client.fetchSubscription(subscriptionId);
        if (!String.valueOf(organizationId).equals(remote.path("notes").path("organizationId").asText())) {
            throw new IllegalArgumentException("This payment belongs to a different workspace.");
        }
        apply(remote);
    }

    @Transactional
    public void cancel(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        requireEnabled();
        Subscription subscription = subscription(organizationId);
        if (!PROVIDER.equals(subscription.getBillingProvider()) || subscription.getRazorpaySubscriptionId() == null) {
            throw new InvalidStateException("This workspace has no Razorpay subscription.");
        }
        client.cancelSubscription(subscription.getRazorpaySubscriptionId(), true);
        subscription.setCancelAtPeriodEnd(true);
        log.info("Razorpay subscription set to cancel at period end: organizationId={}", organizationId);
    }

    /** Ends the subscription right away; used before a workspace is deleted. */
    public void cancelImmediately(Subscription subscription) {
        if (!isEnabled()) {
            log.warn("Razorpay is not configured; subscription left as is: subscriptionId={}",
                    subscription.getRazorpaySubscriptionId());
            return;
        }
        client.cancelSubscription(subscription.getRazorpaySubscriptionId(), false);
    }

    @Transactional
    public void handleWebhook(String payload, String signature, String eventId) {
        if (!isEnabled() || properties.getWebhookSecret().isBlank()) {
            throw new InvalidStateException("Razorpay is not configured");
        }
        if (!signatureMatches(payload, signature, properties.getWebhookSecret())) {
            throw new IllegalArgumentException("Invalid Razorpay signature");
        }
        String recordId = eventId == null || eventId.isBlank() ? null : "razorpay:" + eventId;
        if (recordId != null && eventRepository.existsById(recordId)) {
            return;
        }

        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Malformed Razorpay event");
        }
        String type = event.path("event").asText("");
        JsonNode entity = event.path("payload").path("subscription").path("entity");
        if (type.startsWith("subscription.") && entity.isObject()) {
            Subscription updated = apply(entity);
            if ("subscription.pending".equals(type) && updated != null) {
                notifyOwners(updated.getOrganization().getId(), "Payment failed",
                        "Razorpay could not charge your payment method. Update it so SEOPulse can keep your plan active.");
            }
        }

        if (recordId != null) {
            eventRepository.save(StripeEventRecord.builder()
                    .id(recordId)
                    .type(type.length() > 120 ? type.substring(0, 120) : type)
                    .processedAt(Instant.now())
                    .build());
        }
    }

    /** Re-reads a subscription from Razorpay; used by the nightly reconciliation. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refresh(String razorpaySubscriptionId) {
        apply(client.fetchSubscription(razorpaySubscriptionId));
    }

    private Subscription apply(JsonNode remote) {
        String remoteId = remote.path("id").asText(null);
        SubscriptionStatus status = mapStatus(remote.path("status").asText(""));
        if (remoteId == null || status == null) {
            return null;
        }
        String orgValue = remote.path("notes").path("organizationId").asText(null);
        Subscription subscription = orgValue != null
                ? subscriptionRepository.findByOrganizationId(Long.valueOf(orgValue)).orElse(null)
                : subscriptionRepository.findByRazorpaySubscriptionId(remoteId).orElse(null);
        if (subscription == null) {
            log.warn("Razorpay event for an unknown workspace: subscriptionId={}", remoteId);
            return null;
        }

        boolean terminal = status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID;
        boolean otherActive = isPaid(subscription) && !remoteId.equals(subscription.getRazorpaySubscriptionId());
        if (otherActive && terminal) {
            log.info("Ignoring Razorpay update for a replaced subscription: subscriptionId={}", remoteId);
            return null;
        }

        Long organizationId = subscription.getOrganization().getId();
        subscription.setRazorpaySubscriptionId(remoteId);
        subscription.setStatus(status);
        JsonNode currentEnd = remote.path("current_end");
        if (currentEnd.isNumber()) {
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(currentEnd.asLong()));
        }
        if (status == SubscriptionStatus.PAST_DUE && subscription.getGraceUntil() == null) {
            subscription.setGraceUntil(Instant.now().plus(7, ChronoUnit.DAYS));
        }
        if (status == SubscriptionStatus.ACTIVE) {
            subscription.setGraceUntil(null);
        }

        if (terminal) {
            subscription.setBillingProvider(null);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setPlan(planRepository.findByCode("FREE").orElse(subscription.getPlan()));
            entitlementService.lockExcessWebsites(organizationId);
            notifyOwners(organizationId, "Subscription ended",
                    "SEOPulse moved this workspace to the Free plan. Extra websites are locked, not deleted.");
        } else {
            subscription.setBillingProvider(PROVIDER);
            Plan plan = properties.planCodeFor(remote.path("plan_id").asText(null))
                    .flatMap(planRepository::findByCode)
                    .orElse(subscription.getPlan());
            subscription.setPlan(plan);
        }
        log.info("Razorpay subscription synced: organizationId={}, status={}, plan={}",
                organizationId, status, subscription.getPlan().getCode());
        return subscription;
    }

    private static boolean isPaid(Subscription subscription) {
        return subscription.getBillingProvider() != null
                && subscription.getStatus() != SubscriptionStatus.CANCELED
                && !"FREE".equals(subscription.getPlan().getCode());
    }

    /** Razorpay statuses: created, authenticated, active, pending, halted, paused, cancelled, completed, expired. */
    private static SubscriptionStatus mapStatus(String status) {
        return switch (status) {
            case "authenticated", "active", "resumed" -> SubscriptionStatus.ACTIVE;
            case "pending", "paused" -> SubscriptionStatus.PAST_DUE;
            case "halted" -> SubscriptionStatus.UNPAID;
            case "cancelled", "completed", "expired" -> SubscriptionStatus.CANCELED;
            default -> null;
        };
    }

    static boolean signatureMatches(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }

    private Subscription subscription(Long organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new InvalidStateException("Razorpay is not configured");
        }
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
}
