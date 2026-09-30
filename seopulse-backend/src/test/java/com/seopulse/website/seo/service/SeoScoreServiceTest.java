package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.entity.SeoIssue;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoScoreServiceTest {

    private final SeoIssueRepository repository = mock(SeoIssueRepository.class);
    private final SeoScoreService service = new SeoScoreService(repository);

    @Test
    void pageWithoutIssuesScoresFullMarks() {
        AuditPage page = page(1L);
        when(repository.findByAuditPageId(1L)).thenReturn(List.of());

        assertThat(service.calculatePageScore(page)).isEqualTo(100);
    }

    @Test
    void deductionsDependOnSeverity() {
        AuditPage page = page(1L);
        when(repository.findByAuditPageId(1L)).thenReturn(List.of(
                issue("ERROR"),
                issue("warning"),
                issue("INFO"),
                issue(null),
                issue("UNKNOWN")
        ));

        assertThat(service.calculatePageScore(page)).isEqualTo(100 - 10 - 4 - 1);
    }

    @Test
    void scoreNeverGoesBelowZero() {
        AuditPage page = page(1L);
        List<SeoIssue> issues = new ArrayList<>(Collections.nCopies(15, issue("ERROR")));
        when(repository.findByAuditPageId(1L)).thenReturn(issues);

        assertThat(service.calculatePageScore(page)).isZero();
    }

    @Test
    void auditScoreIsTheRoundedAverageOfPageScores() {
        when(repository.findByAuditPageId(1L)).thenReturn(List.of());
        when(repository.findByAuditPageId(2L)).thenReturn(List.of(issue("ERROR"), issue("WARNING")));
        when(repository.findByAuditPageId(3L)).thenReturn(List.of(issue("WARNING")));

        // (100 + 86 + 96) / 3 = 94
        assertThat(service.calculateAuditScore(List.of(page(1L), page(2L), page(3L)))).isEqualTo(94);
    }

    @Test
    void auditWithoutPagesScoresZero() {
        assertThat(service.calculateAuditScore(List.of())).isZero();
        assertThat(service.calculateAuditScore(null)).isZero();
    }

    private static AuditPage page(Long id) {
        return AuditPage.builder().id(id).build();
    }

    private static SeoIssue issue(String severity) {
        return SeoIssue.builder().severity(severity).build();
    }
}
