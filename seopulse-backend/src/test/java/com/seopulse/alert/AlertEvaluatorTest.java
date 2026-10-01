package com.seopulse.alert;

import com.seopulse.website.comparison.AuditComparison;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.seo.model.IssueSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluatorTest {

    private final Website website = Website.builder().id(1L).name("Example").url("https://example.com").build();

    @Test
    void scoreDropFiresAtTheThreshold() {
        var context = completed(comparison(audit(80), audit(90), List.of()));

        assertThat(AlertEvaluator.evaluate(AlertType.SCORE_DROP, 10, context)).isPresent()
                .get().extracting(AlertEvaluator.Trigger::subject).isEqualTo("Score dropped 10 points on Example");
        assertThat(AlertEvaluator.evaluate(AlertType.SCORE_DROP, 11, context)).isEmpty();
    }

    @Test
    void scoreDropNeedsABaseline() {
        var context = completed(new AuditComparison(audit(40), null, true, List.of(), List.of(), 0, Map.of(), Map.of()));

        assertThat(AlertEvaluator.evaluate(AlertType.SCORE_DROP, 1, context)).isEmpty();
    }

    @Test
    void newErrorsCountsOnlyErrorsAndListsExamples() {
        var issues = List.of(issue("ERROR"), issue("ERROR"), issue("WARNING"));
        var context = completed(comparison(audit(70), audit(72), issues));

        var trigger = AlertEvaluator.evaluate(AlertType.NEW_ERRORS, 2, context);

        assertThat(trigger).isPresent();
        assertThat(trigger.get().details()).containsEntry("newErrors", 2);
        assertThat((List<?>) trigger.get().details().get("examples")).hasSize(2);
        assertThat(AlertEvaluator.evaluate(AlertType.NEW_ERRORS, 3, context)).isEmpty();
    }

    @Test
    void newErrorsIsSilentWhenDetailsWerePurged() {
        var context = completed(new AuditComparison(audit(70), audit(80), false, List.of(), List.of(), 0,
                Map.of("ERROR", 0), Map.of()));

        assertThat(AlertEvaluator.evaluate(AlertType.NEW_ERRORS, 1, context)).isEmpty();
    }

    @Test
    void pageUnreachableReportsTheStatus() {
        Audit audit = audit(0);
        var context = new AlertEvaluator.Context(audit, website, null, false, 503, null);

        var trigger = AlertEvaluator.evaluate(AlertType.PAGE_UNREACHABLE, null, context);

        assertThat(trigger).isPresent();
        assertThat(trigger.get().summary()).contains("HTTP 503");
        assertThat(AlertEvaluator.evaluate(AlertType.PAGE_UNREACHABLE, null,
                new AlertEvaluator.Context(audit, website, null, true, 200, null))).isEmpty();
    }

    @Test
    void auditFailedOnlyFiresForFailedAudits() {
        Audit failed = audit(null);
        failed.setStatus(AuditStatus.FAILED);
        failed.setErrorMessage("Crawl timed out");
        var failedContext = new AlertEvaluator.Context(failed, website, null, true, null, null);

        assertThat(AlertEvaluator.evaluate(AlertType.AUDIT_FAILED, null, failedContext)).isPresent()
                .get().extracting(AlertEvaluator.Trigger::summary).isEqualTo("Crawl timed out");
        assertThat(AlertEvaluator.evaluate(AlertType.SCORE_DROP, 1, failedContext)).isEmpty();
        assertThat(AlertEvaluator.evaluate(AlertType.AUDIT_FAILED, null, completed(null))).isEmpty();
    }

    private AlertEvaluator.Context completed(AuditComparison comparison) {
        Audit audit = comparison == null ? audit(90) : comparison.target();
        return new AlertEvaluator.Context(audit, website, comparison, true, 200, null);
    }

    private static AuditComparison comparison(Audit target, Audit baseline, List<IssueSnapshot> newIssues) {
        long errors = newIssues.stream().filter(issue -> issue.severity().equals("ERROR")).count();
        return new AuditComparison(target, baseline, true, newIssues, List.of(), 0,
                Map.of("ERROR", (int) errors), Map.of());
    }

    private static Audit audit(Integer score) {
        return Audit.builder().id(score == null ? 99L : score.longValue()).score(score).status(AuditStatus.COMPLETED).build();
    }

    private static IssueSnapshot issue(String severity) {
        return new IssueSnapshot("fp-" + Math.random(), "TITLE_MISSING", severity, "CONTENT", "https://example.com/a", "Missing");
    }
}
