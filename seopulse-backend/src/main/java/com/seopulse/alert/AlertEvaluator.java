package com.seopulse.alert;

import com.seopulse.website.comparison.AuditComparison;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.seo.model.IssueSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Decides whether a rule fires for a finished audit. Pure logic, no I/O. */
public final class AlertEvaluator {

    public static final int DEFAULT_SCORE_DROP = 10;
    public static final int DEFAULT_NEW_ERRORS = 1;
    static final int MAX_LISTED_ERRORS = 5;

    private AlertEvaluator() {
    }

    /**
     * @param comparison     null for failed audits
     * @param homepageStatus HTTP status of the start page, null if it could not be fetched
     */
    public record Context(
            Audit audit,
            Website website,
            AuditComparison comparison,
            boolean homepageReachable,
            Integer homepageStatus,
            String homepageError
    ) {
    }

    public record Trigger(String subject, String summary, Map<String, Object> details) {
    }

    public static Optional<Trigger> evaluate(AlertType type, Integer threshold, Context context) {

        Audit audit = context.audit();
        String site = context.website().getName();

        if (type == AlertType.AUDIT_FAILED) {
            if (audit.getStatus() != AuditStatus.FAILED) {
                return Optional.empty();
            }
            String reason = audit.getErrorMessage() == null ? "The audit could not be completed." : audit.getErrorMessage();
            return Optional.of(new Trigger("Audit failed for " + site, reason, Map.of()));
        }

        if (audit.getStatus() != AuditStatus.COMPLETED) {
            return Optional.empty();
        }

        AuditComparison comparison = context.comparison();

        return switch (type) {
            case SCORE_DROP -> {
                if (comparison == null || comparison.scoreDelta() == null) {
                    yield Optional.empty();
                }
                int drop = -comparison.scoreDelta();
                int limit = threshold == null ? DEFAULT_SCORE_DROP : threshold;
                if (drop < limit) {
                    yield Optional.empty();
                }
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("previousScore", comparison.baseline().getScore());
                details.put("score", audit.getScore());
                details.put("drop", drop);
                yield Optional.of(new Trigger(
                        "Score dropped " + drop + " points on " + site,
                        "The SEO score of " + context.website().getUrl() + " fell from "
                                + comparison.baseline().getScore() + " to " + audit.getScore() + ".",
                        details));
            }
            case NEW_ERRORS -> {
                if (comparison == null || comparison.baseline() == null || !comparison.detailsAvailable()) {
                    yield Optional.empty();
                }
                int count = comparison.newErrorCount();
                int limit = threshold == null ? DEFAULT_NEW_ERRORS : threshold;
                if (count < limit) {
                    yield Optional.empty();
                }
                List<Map<String, String>> samples = comparison.newIssues().stream()
                        .filter(issue -> "ERROR".equalsIgnoreCase(issue.severity()))
                        .limit(MAX_LISTED_ERRORS)
                        .map(AlertEvaluator::sample)
                        .toList();
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("newErrors", count);
                details.put("examples", samples);
                yield Optional.of(new Trigger(
                        count + (count == 1 ? " new error" : " new errors") + " on " + site,
                        "The latest audit of " + context.website().getUrl() + " found " + count
                                + (count == 1 ? " error" : " errors") + " that were not there last time.",
                        details));
            }
            case PAGE_UNREACHABLE -> {
                if (context.homepageReachable()) {
                    yield Optional.empty();
                }
                String reason = context.homepageStatus() != null
                        ? "returned HTTP " + context.homepageStatus()
                        : "could not be fetched" + (context.homepageError() == null ? "" : " (" + context.homepageError() + ")");
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("status", context.homepageStatus());
                details.put("error", context.homepageError());
                yield Optional.of(new Trigger(
                        site + " is unreachable",
                        "The start page " + context.website().getUrl() + " " + reason + ".",
                        details));
            }
            case AUDIT_FAILED -> Optional.empty();
        };
    }

    private static Map<String, String> sample(IssueSnapshot issue) {
        Map<String, String> sample = new LinkedHashMap<>();
        sample.put("rule", issue.ruleCode());
        sample.put("url", issue.url());
        sample.put("message", issue.message());
        return sample;
    }
}
