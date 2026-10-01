package com.seopulse.website.seo.rules;

public record RuleDefinition(
        String code,
        RuleCategory category,
        String severity,
        int weight,
        String title,
        String recommendation,
        String helpUrl
) {

    static RuleDefinition from(SeoRule rule) {
        return new RuleDefinition(
                rule.getCode(),
                RuleCategory.valueOf(rule.getCategory()),
                rule.getSeverity(),
                rule.getWeight(),
                rule.getTitle(),
                rule.getRecommendation(),
                rule.getHelpUrl()
        );
    }

    /** For rule codes missing from {@code seo_rules}: weight follows severity. */
    static RuleDefinition fallback(String code, String severity) {
        return new RuleDefinition(
                code,
                RuleCategory.TECHNICAL,
                severity == null ? "INFO" : severity.toUpperCase(),
                defaultWeight(severity),
                code,
                "Review this SEO issue and make the recommended improvement.",
                null
        );
    }

    static int defaultWeight(String severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity.toUpperCase()) {
            case "ERROR" -> 10;
            case "WARNING" -> 4;
            case "INFO" -> 1;
            default -> 0;
        };
    }
}
