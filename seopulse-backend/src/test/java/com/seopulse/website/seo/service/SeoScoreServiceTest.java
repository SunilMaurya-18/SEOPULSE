package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.seo.model.IssueRow;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.SeoRule;
import com.seopulse.website.seo.rules.SeoRuleRepository;
import com.seopulse.website.seo.service.SeoScoreService.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoScoreServiceTest {

    private final SeoRuleRepository ruleRepository = mock(SeoRuleRepository.class);
    private final SeoScoreService service = new SeoScoreService(new RuleCatalog(ruleRepository));

    SeoScoreServiceTest() {
        when(ruleRepository.findAll()).thenReturn(List.of(
                rule("TITLE_MISSING", "CONTENT", "ERROR", 12),
                rule("HSTS_MISSING", "SECURITY", "WARNING", 3)
        ));
    }

    @Test
    void pagesWithoutIssuesScoreFullMarks() {
        ScoreResult result = service.score(List.of(page(1L, 0), page(2L, 1)), List.of(), Map.of());

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.categoryScores()).containsEntry("CONTENT", 100).containsEntry("SECURITY", 100);
        assertThat(result.issueCount()).isZero();
    }

    @Test
    void catalogWeightsAreDeductedAndUnknownRulesFallBackToSeverity() {
        List<IssueRow> issues = List.of(
                new IssueRow(1L, "TITLE_MISSING", "ERROR", "CONTENT"),
                new IssueRow(1L, "HSTS_MISSING", "WARNING", "SECURITY"),
                new IssueRow(1L, "SOMETHING_NEW", "INFO", null)
        );

        ScoreResult result = service.score(List.of(page(1L, 0)), issues, Map.of());

        assertThat(result.score()).isEqualTo(100 - 12 - 3 - 1);
        assertThat(result.categoryScores())
                .containsEntry("CONTENT", 88)
                .containsEntry("SECURITY", 97)
                .containsEntry("TECHNICAL", 99);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.warningCount()).isEqualTo(1);
        assertThat(result.infoCount()).isEqualTo(1);
    }

    @Test
    void pageScoreNeverGoesBelowZero() {
        List<IssueRow> issues = new ArrayList<>(Collections.nCopies(15, new IssueRow(1L, "TITLE_MISSING", "ERROR", "CONTENT")));

        assertThat(service.score(List.of(page(1L, 0)), issues, Map.of()).score()).isZero();
    }

    @Test
    void homepageIssuesWeighMoreThanDeepPageIssues() {
        List<AuditPage> pages = List.of(page(1L, 0), page(2L, 4));

        int homepageBroken = service.score(pages, List.of(new IssueRow(1L, "TITLE_MISSING", "ERROR", "CONTENT")), Map.of()).score();
        int deepPageBroken = service.score(pages, List.of(new IssueRow(2L, "TITLE_MISSING", "ERROR", "CONTENT")), Map.of()).score();

        assertThat(homepageBroken).isLessThan(deepPageBroken);
    }

    @Test
    void importanceGrowsWithInboundLinksUpToDouble() {
        AuditPage page = page(1L, 2);

        assertThat(SeoScoreService.importance(page, 0)).isCloseTo(1 + 2.0 / 3, within(1e-9));
        assertThat(SeoScoreService.importance(page, 10)).isCloseTo((1 + 2.0 / 3) * 1.5, within(1e-9));
        assertThat(SeoScoreService.importance(page, 500)).isCloseTo((1 + 2.0 / 3) * 2, within(1e-9));
    }

    @Test
    void skippedPagesOnlyCountWhenTheyHaveIssues() {
        AuditPage skipped = page(2L, 1);
        skipped.setStatus(AuditPageStatus.SKIPPED_ROBOTS);

        assertThat(service.score(List.of(page(1L, 0), skipped), List.of(), Map.of()).score()).isEqualTo(100);

        AuditPage redirect = page(3L, 1);
        redirect.setStatus(AuditPageStatus.REDIRECT);
        ScoreResult result = service.score(
                List.of(page(1L, 0), redirect),
                List.of(new IssueRow(3L, "TITLE_MISSING", "ERROR", "CONTENT")),
                Map.of());
        assertThat(result.score()).isLessThan(100);
    }

    @Test
    void auditWithoutScorablePagesScoresZero() {
        assertThat(service.score(List.of(), List.of(), Map.of()).score()).isZero();
    }

    private static AuditPage page(Long id, int depth) {
        return AuditPage.builder()
                .id(id)
                .url("https://example.com/" + id)
                .depth(depth)
                .status(AuditPageStatus.CRAWLED)
                .build();
    }

    private static SeoRule rule(String code, String category, String severity, int weight) {
        return SeoRule.builder()
                .code(code)
                .category(category)
                .severity(severity)
                .weight(weight)
                .title(code)
                .recommendation("Fix it")
                .build();
    }
}
