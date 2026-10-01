package com.seopulse.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.service.SecureTokens;
import com.seopulse.billing.EntitlementService;
import com.seopulse.billing.PlanLimits;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.organization.service.OrganizationAccessService;
import com.seopulse.website.comparison.AuditComparison;
import com.seopulse.website.comparison.AuditComparisonService;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import com.seopulse.website.service.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    static final int MAX_RULES_PER_ORG = 50;
    static final String SLACK_PREFIX = "https://hooks.slack.com/";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AlertRuleRepository ruleRepository;
    private final AlertOutboxRepository outboxRepository;
    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final WebsiteRepository websiteRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditComparisonService comparisonService;
    private final OrganizationAccessService accessService;
    private final EntitlementService entitlementService;
    private final UrlValidator urlValidator;
    private final AlertDelivery delivery;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    // ---- evaluation ---------------------------------------------------------

    /** Queues an alert for every applicable rule that fires for the finished audit. */
    @Transactional
    public int evaluate(Long auditId) {

        Audit audit = auditRepository.findByIdWithWebsite(auditId).orElse(null);
        if (audit == null || (audit.getStatus() != AuditStatus.COMPLETED && audit.getStatus() != AuditStatus.FAILED)) {
            return 0;
        }

        Website website = audit.getWebsite();
        Long organizationId = website.getProject().getOrganization().getId();
        List<AlertRule> rules = ruleRepository.findApplicable(organizationId, website.getId());
        if (rules.isEmpty()) {
            return 0;
        }

        PlanLimits limits = entitlementService.limitsFor(organizationId);
        AlertEvaluator.Context context = context(audit, website, rules);
        int queued = 0;

        for (AlertRule rule : rules) {
            if (rule.getChannel().isWebhook() && !limits.webhookAlerts()) {
                continue;
            }
            var trigger = AlertEvaluator.evaluate(rule.getType(), rule.getThreshold(), context);
            if (trigger.isEmpty()
                    || outboxRepository.existsByAlertRuleIdAndAuditIdAndEventType(rule.getId(), auditId, rule.getType().name())) {
                continue;
            }
            outboxRepository.save(AlertOutbox.builder()
                    .alertRuleId(rule.getId())
                    .organizationId(organizationId)
                    .auditId(auditId)
                    .eventType(rule.getType().name())
                    .channel(rule.getChannel())
                    .target(rule.getTarget())
                    .subject(truncate(trigger.get().subject(), 200))
                    .payload(payload(rule.getType().name(), organizationId, website, audit, context.comparison(), trigger.get()))
                    .build());
            queued++;
        }

        if (queued > 0) {
            log.info("Alerts queued: auditId={}, count={}", auditId, queued);
        }
        return queued;
    }

    private AlertEvaluator.Context context(Audit audit, Website website, List<AlertRule> rules) {

        if (audit.getStatus() != AuditStatus.COMPLETED) {
            return new AlertEvaluator.Context(audit, website, null, true, null, null);
        }

        boolean needsComparison = rules.stream()
                .anyMatch(rule -> rule.getType() == AlertType.SCORE_DROP || rule.getType() == AlertType.NEW_ERRORS);
        AuditComparison comparison = needsComparison ? comparisonService.compareWithPrevious(audit.getId()) : null;

        List<AuditPage> startPages = auditPageRepository.findByAuditIdAndDepthOrderByIdAsc(audit.getId(), 0);
        boolean reachable = startPages.stream().anyMatch(page ->
                page.getStatus() == AuditPageStatus.CRAWLED
                        && page.getStatusCode() != null && page.getStatusCode() < 400);
        // After a redirect the last start-page row is the final hop.
        AuditPage finalHop = startPages.isEmpty() ? null : startPages.getLast();

        return new AlertEvaluator.Context(
                audit,
                website,
                comparison,
                reachable,
                finalHop == null ? null : finalHop.getStatusCode(),
                finalHop == null ? "no response" : finalHop.getSkipReason()
        );
    }

    private String payload(
            String eventType,
            Long organizationId,
            Website website,
            Audit audit,
            AuditComparison comparison,
            AlertEvaluator.Trigger trigger
    ) {
        Map<String, Object> websiteJson = new LinkedHashMap<>();
        websiteJson.put("id", website.getId());
        websiteJson.put("name", website.getName());
        websiteJson.put("url", website.getUrl());

        Map<String, Object> auditJson = new LinkedHashMap<>();
        auditJson.put("id", audit.getId());
        auditJson.put("status", audit.getStatus().name());
        auditJson.put("score", audit.getScore());
        auditJson.put("previousScore", comparison == null || comparison.baseline() == null ? null : comparison.baseline().getScore());
        auditJson.put("triggeredBy", audit.getTriggeredBy() == null ? null : audit.getTriggeredBy().name());
        auditJson.put("completedAt", audit.getCompletedAt() == null ? null : audit.getCompletedAt().toString());
        auditJson.put("url", authProperties.getAppBaseUrl() + "/audits/" + audit.getId());

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", eventType);
        event.put("occurredAt", Instant.now().toString());
        event.put("organizationId", organizationId);
        event.put("website", websiteJson);
        event.put("audit", auditJson);
        event.put("summary", trigger.summary());
        event.put("details", trigger.details());
        return json(event);
    }

    // ---- defaults -----------------------------------------------------------

    /** Creates the default email rules the first time an organization adds a website. */
    @Transactional
    public void applyDefaults(Long organizationId) {
        if (organizationRepository.claimAlertDefaults(organizationId) == 0) {
            return;
        }
        ruleRepository.save(AlertRule.builder()
                .organizationId(organizationId)
                .type(AlertType.SCORE_DROP)
                .threshold(AlertEvaluator.DEFAULT_SCORE_DROP)
                .channel(AlertChannel.EMAIL)
                .enabled(true)
                .build());
        ruleRepository.save(AlertRule.builder()
                .organizationId(organizationId)
                .type(AlertType.NEW_ERRORS)
                .threshold(AlertEvaluator.DEFAULT_NEW_ERRORS)
                .channel(AlertChannel.EMAIL)
                .enabled(true)
                .build());
    }

    // ---- management ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> list(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        return ruleRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .map(rule -> AlertRuleResponse.from(rule, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertDeliveryResponse> deliveries(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        return outboxRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(AlertDeliveryResponse::from)
                .toList();
    }

    @Transactional
    public AlertRuleResponse create(Long organizationId, Long userId, AlertRuleRequest request) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        if (ruleRepository.countByOrganizationId(organizationId) >= MAX_RULES_PER_ORG) {
            throw new IllegalArgumentException("An organization can have at most " + MAX_RULES_PER_ORG + " alert rules");
        }

        AlertRule rule = AlertRule.builder().organizationId(organizationId).build();
        apply(rule, organizationId, request);
        if (rule.getChannel() == AlertChannel.WEBHOOK) {
            rule.setSigningSecret(newSecret());
        }
        return AlertRuleResponse.from(ruleRepository.save(rule), true);
    }

    @Transactional
    public AlertRuleResponse update(Long organizationId, Long ruleId, Long userId, AlertRuleRequest request) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        AlertRule rule = requireRule(organizationId, ruleId);
        apply(rule, organizationId, request);
        boolean revealSecret = false;
        if (rule.getChannel() == AlertChannel.WEBHOOK && rule.getSigningSecret() == null) {
            rule.setSigningSecret(newSecret());
            revealSecret = true;
        } else if (rule.getChannel() != AlertChannel.WEBHOOK) {
            rule.setSigningSecret(null);
        }
        return AlertRuleResponse.from(rule, revealSecret);
    }

    @Transactional
    public AlertRuleResponse rotateSecret(Long organizationId, Long ruleId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        AlertRule rule = requireRule(organizationId, ruleId);
        if (rule.getChannel() != AlertChannel.WEBHOOK) {
            throw new IllegalArgumentException("Only webhook rules have a signing secret");
        }
        rule.setSigningSecret(newSecret());
        return AlertRuleResponse.from(rule, true);
    }

    @Transactional
    public void delete(Long organizationId, Long ruleId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        ruleRepository.delete(requireRule(organizationId, ruleId));
    }

    /** Sends a sample alert through the rule's channel right away and reports the outcome. */
    @Transactional(readOnly = true)
    public TestAlertResult sendTest(Long organizationId, Long ruleId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        AlertRule rule = requireRule(organizationId, ruleId);
        if (rule.getChannel().isWebhook()) {
            entitlementService.requireWebhookAlerts(organizationId);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("test", true);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "TEST");
        event.put("occurredAt", Instant.now().toString());
        event.put("organizationId", organizationId);
        event.put("rule", Map.of("id", rule.getId(), "type", rule.getType().name()));
        event.put("summary", "This is a test alert from SEOPulse. If you can read this, the "
                + label(rule.getChannel()) + " channel works.");
        event.put("details", details);
        event.put("audit", Map.of("url", authProperties.getAppBaseUrl() + "/settings"));

        try {
            delivery.deliver(organizationId, rule.getChannel(), rule.getTarget(), rule.getSigningSecret(),
                    "TEST", UUID.randomUUID().toString(), "Test alert", json(event));
            return new TestAlertResult(true, null);
        } catch (AlertDelivery.AlertDeliveryException ex) {
            return new TestAlertResult(false, truncate(ex.getMessage(), 300));
        }
    }

    public record TestAlertResult(boolean delivered, String error) {
    }

    private void apply(AlertRule rule, Long organizationId, AlertRuleRequest request) {

        AlertChannel channel = request.channel();
        if (channel.isWebhook()) {
            entitlementService.requireWebhookAlerts(organizationId);
        }

        if (request.websiteId() != null) {
            Website website = websiteRepository.findById(request.websiteId())
                    .filter(item -> item.getProject().getOrganization().getId().equals(organizationId))
                    .orElseThrow(() -> new ResourceNotFoundException("Website not found"));
            rule.setWebsiteId(website.getId());
        } else {
            rule.setWebsiteId(null);
        }

        boolean keepTarget = rule.getId() != null
                && rule.getChannel() == channel
                && channel.isWebhook()
                && (request.target() == null || request.target().isBlank() || request.target().contains("…"));

        rule.setType(request.type());
        rule.setThreshold(threshold(request.type(), request.threshold()));
        rule.setChannel(channel);
        if (!keepTarget) {
            rule.setTarget(target(channel, request.target()));
        }
        rule.setEnabled(request.enabled() == null || request.enabled());
    }

    private static Integer threshold(AlertType type, Integer requested) {
        return switch (type) {
            case SCORE_DROP -> {
                int value = requested == null ? AlertEvaluator.DEFAULT_SCORE_DROP : requested;
                if (value < 1 || value > 100) {
                    throw new IllegalArgumentException("Score drop threshold must be between 1 and 100");
                }
                yield value;
            }
            case NEW_ERRORS -> {
                int value = requested == null ? AlertEvaluator.DEFAULT_NEW_ERRORS : requested;
                if (value < 1 || value > 10_000) {
                    throw new IllegalArgumentException("New error threshold must be between 1 and 10000");
                }
                yield value;
            }
            case PAGE_UNREACHABLE, AUDIT_FAILED -> null;
        };
    }

    private String target(AlertChannel channel, String raw) {
        String value = raw == null ? null : raw.trim();
        return switch (channel) {
            case EMAIL -> {
                if (value == null || value.isEmpty()) {
                    yield null;
                }
                if (value.length() > 254 || !EMAIL.matcher(value).matches()) {
                    throw new IllegalArgumentException("Enter a valid email address");
                }
                yield value;
            }
            case SLACK_WEBHOOK -> {
                if (value == null || !value.startsWith(SLACK_PREFIX)) {
                    throw new IllegalArgumentException("Slack webhook URLs start with " + SLACK_PREFIX);
                }
                urlValidator.validateStructure(value);
                yield value;
            }
            case WEBHOOK -> {
                if (value == null || !value.regionMatches(true, 0, "https://", 0, 8)) {
                    throw new IllegalArgumentException("Webhook URLs must use HTTPS");
                }
                urlValidator.validate(value);
                yield value;
            }
        };
    }

    private AlertRule requireRule(Long organizationId, Long ruleId) {
        return ruleRepository.findByIdAndOrganizationId(ruleId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found"));
    }

    private static String newSecret() {
        return "whsec_" + SecureTokens.generate();
    }

    private static String label(AlertChannel channel) {
        return switch (channel) {
            case EMAIL -> "email";
            case SLACK_WEBHOOK -> "Slack";
            case WEBHOOK -> "webhook";
        };
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize alert", ex);
        }
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
