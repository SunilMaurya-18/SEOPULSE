package com.seopulse.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders an alert event for its channel and sends it. */
@Component
@RequiredArgsConstructor
public class AlertDelivery {

    private final WebhookHttpClient webhookClient;
    private final EmailOutboxService emailOutboxService;
    private final OrganizationMemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    /**
     * @param deliveryId unique ID sent as {@code X-SEOPulse-Delivery}, so receivers can drop duplicates
     * @throws AlertDeliveryException when the receiver rejects the alert or cannot be reached
     */
    public void deliver(
            Long organizationId,
            AlertChannel channel,
            String target,
            String signingSecret,
            String eventType,
            String deliveryId,
            String subject,
            String payload
    ) {
        switch (channel) {
            case EMAIL -> sendEmail(organizationId, target, subject, payload);
            case SLACK_WEBHOOK -> post(target, slackBody(subject, payload), Map.of());
            case WEBHOOK -> {
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("X-SEOPulse-Event", eventType);
                headers.put("X-SEOPulse-Delivery", deliveryId);
                if (signingSecret != null) {
                    headers.put(WebhookSigner.HEADER, WebhookSigner.sign(signingSecret, Instant.now().getEpochSecond(), payload));
                }
                post(target, payload, headers);
            }
        }
    }

    private void post(String url, String body, Map<String, String> headers) {
        try {
            WebhookHttpClient.Result result = webhookClient.postJson(url, body, headers);
            if (!result.isSuccess()) {
                throw new AlertDeliveryException("Receiver responded with HTTP " + result.status());
            }
        } catch (IOException ex) {
            throw new AlertDeliveryException(ex.getMessage() == null ? "Webhook request failed" : ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AlertDeliveryException("Interrupted");
        } catch (IllegalArgumentException ex) {
            throw new AlertDeliveryException(ex.getMessage());
        }
    }

    private void sendEmail(Long organizationId, String target, String subject, String payload) {
        JsonNode event = read(payload);
        String summary = event.path("summary").asText("");
        String link = event.path("audit").path("url").asText(null);
        String text = EmailTemplates.text(summary + details(event), link);
        String html = EmailTemplates.html(subject, summary, link == null ? null : "View audit", link);

        List<String> recipients = target != null && !target.isBlank()
                ? List.of(target.trim())
                : adminEmails(organizationId);
        if (recipients.isEmpty()) {
            throw new AlertDeliveryException("The organization has no owners or admins to email");
        }
        for (String to : recipients) {
            emailOutboxService.enqueue(to, "[SEOPulse] " + subject, text, html);
        }
    }

    private List<String> adminEmails(Long organizationId) {
        return memberRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .filter(member -> member.getRole().atLeast(OrganizationRole.ADMIN))
                .map(OrganizationMember::getUser)
                .map(user -> user.getEmail())
                .distinct()
                .toList();
    }

    String slackBody(String subject, String payload) {
        JsonNode event = read(payload);
        StringBuilder text = new StringBuilder("*SEOPulse* | ").append(escapeSlack(subject))
                .append('\n').append(escapeSlack(event.path("summary").asText("")));
        for (JsonNode example : event.path("details").path("examples")) {
            text.append("\n• ").append(escapeSlack(example.path("rule").asText("")))
                    .append(" on ").append(escapeSlack(example.path("url").asText("")));
        }
        String link = event.path("audit").path("url").asText(null);
        if (link != null) {
            text.append("\n<").append(link).append("|View audit>");
        }
        try {
            return objectMapper.writeValueAsString(Map.of("text", text.toString()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String details(JsonNode event) {
        StringBuilder out = new StringBuilder();
        for (JsonNode example : event.path("details").path("examples")) {
            out.append("\n- ").append(example.path("rule").asText(""))
                    .append(": ").append(example.path("url").asText(""));
        }
        return out.toString();
    }

    private JsonNode read(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new AlertDeliveryException("Stored alert payload is not valid JSON");
        }
    }

    private static String escapeSlack(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static class AlertDeliveryException extends RuntimeException {
        public AlertDeliveryException(String message) {
            super(message);
        }
    }
}
