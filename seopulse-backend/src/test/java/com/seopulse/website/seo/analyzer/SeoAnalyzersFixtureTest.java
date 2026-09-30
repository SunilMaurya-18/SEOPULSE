package com.seopulse.website.seo.analyzer;

import com.seopulse.website.crawler.PageExtractor;
import com.seopulse.website.crawler.SiteScope;
import com.seopulse.website.crawler.UrlNormalizer;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real extraction and every analyzer over HTML fixtures, the
 * same path a crawled page takes.
 */
class SeoAnalyzersFixtureTest {

    private static final String BASE_URL = "https://example.com/wallets";

    private final PageExtractor extractor = new PageExtractor(new UrlNormalizer());

    private final List<SeoAnalyzer> analyzers = List.of(
            new TitleAnalyzer(),
            new MetaDescriptionAnalyzer(),
            new H1Analyzer(),
            new CanonicalAnalyzer(),
            new HttpStatusAnalyzer(),
            new ContentAnalyzer(),
            new ImageAnalyzer(),
            new LinkAnalyzer()
    );

    @Test
    void wellOptimizedPageHasNoIssues() throws IOException {

        PageExtractor.PageContent content = extract("well-optimized.html");

        assertThat(content.title()).isEqualTo("Handmade Leather Wallets | Example Leather Co.");
        assertThat(content.canonicalUrl()).isEqualTo("https://example.com/wallets");
        assertThat(content.h1Count()).isEqualTo(1);
        assertThat(content.imageCount()).isEqualTo(2);
        assertThat(content.imagesWithoutAlt()).isZero();
        assertThat(content.internalLinkCount()).isEqualTo(3);
        assertThat(content.externalLinkCount()).isEqualTo(1);
        assertThat(content.wordCount()).isGreaterThanOrEqualTo(300);

        assertThat(ruleCodes(toPage(content, 200))).isEmpty();
    }

    @Test
    void poorlyOptimizedPageReportsEachProblem() throws IOException {

        PageExtractor.PageContent content = extract("poorly-optimized.html");

        assertThat(content.metaDescription()).isNull();
        assertThat(content.imagesWithoutAlt()).isEqualTo(2);
        assertThat(content.internalLinkCount()).isZero();

        assertThat(ruleCodes(toPage(content, 200))).containsExactlyInAnyOrder(
                "TITLE_TOO_SHORT",
                "META_DESCRIPTION_MISSING",
                "MULTIPLE_H1",
                "CANONICAL_MISSING",
                "LOW_WORD_COUNT",
                "IMAGE_ALT_MISSING",
                "NO_INTERNAL_LINKS"
        );
    }

    @Test
    void pageWithEmptyHeadIsMissingEverything() throws IOException {

        assertThat(ruleCodes(toPage(extract("empty-head.html"), 200))).containsExactlyInAnyOrder(
                "TITLE_MISSING",
                "META_DESCRIPTION_MISSING",
                "H1_MISSING",
                "CANONICAL_MISSING",
                "LOW_WORD_COUNT"
        );
    }

    @Test
    void errorStatusesAreReported() throws IOException {

        PageExtractor.PageContent content = extract("well-optimized.html");

        assertThat(ruleCodes(toPage(content, 404))).containsExactly("HTTP_4XX");
        assertThat(ruleCodes(toPage(content, 503))).containsExactly("HTTP_5XX");
    }

    private PageExtractor.PageContent extract(String fixture) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/html/" + fixture)) {
            assertThat(in).as("fixture " + fixture).isNotNull();
            Document document = PageExtractor.parse(in.readAllBytes(), "UTF-8", BASE_URL);
            return extractor.extract(document, new SiteScope("example.com")::contains);
        }
    }

    private static AuditPage toPage(PageExtractor.PageContent content, int status) {
        return AuditPage.builder()
                .url(BASE_URL)
                .statusCode(status)
                .title(content.title())
                .metaDescription(content.metaDescription())
                .canonicalUrl(content.canonicalUrl())
                .wordCount(content.wordCount())
                .h1Count(content.h1Count())
                .imageCount(content.imageCount())
                .imagesWithoutAlt(content.imagesWithoutAlt())
                .internalLinkCount(content.internalLinkCount())
                .externalLinkCount(content.externalLinkCount())
                .build();
    }

    private List<String> ruleCodes(AuditPage page) {
        return analyzers.stream()
                .flatMap(analyzer -> analyzer.analyze(page).stream())
                .map(SeoIssueResult::ruleCode)
                .toList();
    }
}
