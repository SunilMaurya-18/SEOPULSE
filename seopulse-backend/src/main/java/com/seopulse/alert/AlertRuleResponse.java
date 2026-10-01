package com.seopulse.alert;

import java.time.Instant;

/**
 * @param signingSecret the full secret only right after it is created or
 *                      rotated; otherwise a masked hint, or null
 */
public record AlertRuleResponse(
        Long id,
        Long websiteId,
        AlertType type,
        Integer threshold,
        AlertChannel channel,
        String target,
        boolean enabled,
        String signingSecret,
        boolean secretRevealed,
        Instant createdAt
) {

    static AlertRuleResponse from(AlertRule rule, boolean revealSecret) {
        String secret = rule.getSigningSecret();
        String shown = secret == null ? null : revealSecret ? secret : mask(secret);
        return new AlertRuleResponse(
                rule.getId(),
                rule.getWebsiteId(),
                rule.getType(),
                rule.getThreshold(),
                rule.getChannel(),
                rule.getChannel() == AlertChannel.SLACK_WEBHOOK ? maskUrl(rule.getTarget()) : rule.getTarget(),
                rule.isEnabled(),
                shown,
                revealSecret && secret != null,
                rule.getCreatedAt()
        );
    }

    private static String mask(String secret) {
        return secret.length() <= 10 ? "whsec_…" : "whsec_…" + secret.substring(secret.length() - 4);
    }

    /** Slack webhook URLs are bearer credentials; show only enough to recognise them. */
    private static String maskUrl(String url) {
        if (url == null || url.length() < 40) {
            return url;
        }
        return url.substring(0, 34) + "…" + url.substring(url.length() - 4);
    }
}
