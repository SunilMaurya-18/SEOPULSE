package com.seopulse.website.comparison;

import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.model.IssueSnapshot;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.seo.rules.IssueFingerprint;
import com.seopulse.website.seo.rules.RuleCatalog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditComparisonServiceTest {

    private final SeoIssueRepository issues = mock(SeoIssueRepository.class);
    private final AuditComparisonService service = new AuditComparisonService(
            mock(AuditRepository.class), issues, mock(ProjectAccessService.class), mock(RuleCatalog.class));

    @Test
    void splitsIssuesIntoNewFixedAndPersisting() {
        Audit base = audit(1L, 80);
        Audit target = audit(2L, 70);
        when(issues.findSnapshotsByAuditId(1L)).thenReturn(List.of(
                snapshot("TITLE_MISSING", "https://example.com/a", "ERROR"),
                snapshot("H1_MISSING", "https://example.com/b", "WARNING")
        ));
        when(issues.findSnapshotsByAuditId(2L)).thenReturn(List.of(
                // Same page after an http -> https / www move: still the same issue.
                snapshot("TITLE_MISSING", "http://www.example.com/a/", "ERROR"),
                snapshot("HTTP_5XX", "https://example.com/c", "ERROR"),
                snapshot("LOW_WORD_COUNT", "https://example.com/d", "INFO")
        ));

        AuditComparison result = service.compare(target, base);

        assertThat(result.detailsAvailable()).isTrue();
        assertThat(result.persistingCount()).isEqualTo(1);
        assertThat(result.newIssues()).extracting(IssueSnapshot::ruleCode).containsExactly("HTTP_5XX", "LOW_WORD_COUNT");
        assertThat(result.fixedIssues()).extracting(IssueSnapshot::ruleCode).containsExactly("H1_MISSING");
        assertThat(result.newErrorCount()).isEqualTo(1);
        assertThat(result.scoreDelta()).isEqualTo(-10);
    }

    @Test
    void withoutABaselineNothingIsNew() {
        AuditComparison result = service.compare(audit(2L, 70), null);

        assertThat(result.baseline()).isNull();
        assertThat(result.newIssues()).isEmpty();
        assertThat(result.scoreDelta()).isNull();
    }

    @Test
    void purgedAuditsReportDetailsUnavailable() {
        Audit base = audit(1L, 80);
        base.setDetailsPurgedAt(Instant.now());

        AuditComparison result = service.compare(audit(2L, 85), base);

        assertThat(result.detailsAvailable()).isFalse();
        assertThat(result.scoreDelta()).isEqualTo(5);
    }

    @Test
    void fingerprintIgnoresSchemeWwwAndTrailingSlash() {
        assertThat(IssueFingerprint.of("X", "https://example.com/path/"))
                .isEqualTo(IssueFingerprint.of("X", "http://www.example.com/path"))
                .isNotEqualTo(IssueFingerprint.of("Y", "https://example.com/path"))
                .isNotEqualTo(IssueFingerprint.of("X", "https://example.com/other"))
                .hasSize(64);
    }

    private static Audit audit(Long id, int score) {
        return Audit.builder().id(id).score(score).build();
    }

    private static IssueSnapshot snapshot(String rule, String url, String severity) {
        return new IssueSnapshot(IssueFingerprint.of(rule, url), rule, severity, null, url, "message");
    }
}
