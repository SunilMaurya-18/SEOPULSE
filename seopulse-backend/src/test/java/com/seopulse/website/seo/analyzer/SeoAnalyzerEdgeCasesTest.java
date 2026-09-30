package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeoAnalyzerEdgeCasesTest {

    @ParameterizedTest
    @CsvSource({
            "29, TITLE_TOO_SHORT",
            "30, ''",
            "60, ''",
            "61, TITLE_TOO_LONG"
    })
    void titleLengthBoundaries(int length, String expected) {
        AuditPage page = AuditPage.builder().title("x".repeat(length)).build();
        assertThat(codes(new TitleAnalyzer().analyze(page))).isEqualTo(expectedList(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "69, META_DESCRIPTION_TOO_SHORT",
            "70, ''",
            "160, ''",
            "161, META_DESCRIPTION_TOO_LONG"
    })
    void metaDescriptionLengthBoundaries(int length, String expected) {
        AuditPage page = AuditPage.builder().metaDescription("x".repeat(length)).build();
        assertThat(codes(new MetaDescriptionAnalyzer().analyze(page))).isEqualTo(expectedList(expected));
    }

    @Test
    void presentMetaDescriptionIsNotReportedMissing() {
        // Regression: the null check was inverted, flagging every page that had a description.
        AuditPage page = AuditPage.builder()
                .metaDescription("A perfectly reasonable description that is comfortably within the limits.")
                .build();
        assertThat(new MetaDescriptionAnalyzer().analyze(page)).isEmpty();
    }

    @Test
    void missingMetaDescriptionIsReported() {
        assertThat(codes(new MetaDescriptionAnalyzer().analyze(AuditPage.builder().build())))
                .containsExactly("META_DESCRIPTION_MISSING");
        assertThat(codes(new MetaDescriptionAnalyzer().analyze(AuditPage.builder().metaDescription(" ").build())))
                .containsExactly("META_DESCRIPTION_MISSING");
    }

    @Test
    void unknownNumericFieldsAreSkippedInsteadOfCrashing() {
        AuditPage page = AuditPage.builder().build();

        assertThat(new HttpStatusAnalyzer().analyze(page)).isEmpty();
        assertThat(new ContentAnalyzer().analyze(page)).isEmpty();
        assertThat(new H1Analyzer().analyze(page)).isEmpty();
        assertThat(new ImageAnalyzer().analyze(page)).isEmpty();
        assertThat(new LinkAnalyzer().analyze(page)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "200, ''",
            "301, ''",
            "400, HTTP_4XX",
            "499, HTTP_4XX",
            "500, HTTP_5XX",
            "599, HTTP_5XX"
    })
    void httpStatusRanges(int status, String expected) {
        AuditPage page = AuditPage.builder().statusCode(status).build();
        assertThat(codes(new HttpStatusAnalyzer().analyze(page))).isEqualTo(expectedList(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "0, H1_MISSING",
            "1, ''",
            "2, MULTIPLE_H1"
    })
    void h1Counts(int count, String expected) {
        AuditPage page = AuditPage.builder().h1Count(count).build();
        assertThat(codes(new H1Analyzer().analyze(page))).isEqualTo(expectedList(expected));
    }

    @Test
    void wordCountThreshold() {
        assertThat(codes(new ContentAnalyzer().analyze(AuditPage.builder().wordCount(299).build())))
                .containsExactly("LOW_WORD_COUNT");
        assertThat(new ContentAnalyzer().analyze(AuditPage.builder().wordCount(300).build())).isEmpty();
    }

    private static List<String> codes(List<SeoIssueResult> results) {
        return results.stream().map(SeoIssueResult::ruleCode).toList();
    }

    private static List<String> expectedList(String expected) {
        return expected == null || expected.isBlank() ? List.of() : List.of(expected);
    }
}
