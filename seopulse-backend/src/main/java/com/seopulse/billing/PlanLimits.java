package com.seopulse.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @param schedule      most frequent audit schedule allowed: NONE, WEEKLY or DAILY
 * @param webhookAlerts whether alerts may be sent to Slack and webhooks (email is always allowed)
 * @param whiteLabel    whether reports may carry the organization's branding instead of SEOPulse's
 * @param retentionDays how long page-level audit details are kept
 */
public record PlanLimits(
        int websites,
        int pagesPerAudit,
        int auditsPerMonth,
        int members,
        String schedule,
        boolean webhookAlerts,
        boolean whiteLabel,
        int retentionDays
) {

    public static final PlanLimits FALLBACK = new PlanLimits(1, 100, 5, 1, "NONE", false, false, 30);

    public static PlanLimits parse(String json, ObjectMapper mapper) {
        try {
            JsonNode node = mapper.readTree(json);
            return new PlanLimits(
                    node.path("websites").asInt(FALLBACK.websites()),
                    node.path("pagesPerAudit").asInt(FALLBACK.pagesPerAudit()),
                    node.path("auditsPerMonth").asInt(FALLBACK.auditsPerMonth()),
                    node.path("members").asInt(FALLBACK.members()),
                    node.path("schedule").asText(FALLBACK.schedule()),
                    node.path("webhookAlerts").asBoolean(FALLBACK.webhookAlerts()),
                    node.path("whiteLabel").asBoolean(FALLBACK.whiteLabel()),
                    node.path("retentionDays").asInt(FALLBACK.retentionDays())
            );
        } catch (Exception ex) {
            return FALLBACK;
        }
    }

    public boolean allowsSchedule(String frequency) {
        return switch (schedule) {
            case "DAILY" -> "DAILY".equals(frequency) || "WEEKLY".equals(frequency);
            case "WEEKLY" -> "WEEKLY".equals(frequency);
            default -> false;
        };
    }
}
