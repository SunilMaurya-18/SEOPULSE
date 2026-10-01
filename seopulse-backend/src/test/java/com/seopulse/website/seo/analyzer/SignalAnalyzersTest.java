package com.seopulse.website.seo.analyzer;

import com.seopulse.website.crawler.PageExtractor;
import com.seopulse.website.crawler.SiteScope;
import com.seopulse.website.crawler.UrlNormalizer;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.analyzer.site.BrokenLinkAnalyzer;
import com.seopulse.website.seo.analyzer.site.DuplicateTitleAnalyzer;
import com.seopulse.website.seo.analyzer.site.OrphanPageAnalyzer;
import com.seopulse.website.seo.analyzer.site.SiteContext;
import com.seopulse.website.seo.analyzer.site.SiteIssue;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Extraction of the Phase 5 signals and the analyzers that use them. */
class SignalAnalyzersTest {

    private static final String URL = "https://example.com/";

    private final PageExtractor extractor = new PageExtractor(new UrlNormalizer());

    private final List<SeoAnalyzer> analyzers = List.of(
            new OpenGraphAnalyzer(),
            new StructuredDataAnalyzer(),
            new ViewportAnalyzer(),
            new MixedContentAnalyzer(),
            new HreflangAnalyzer(),
            new IndexabilityAnalyzer(),
            new LargePageAnalyzer()
    );

    @Test
    void completeHeadProducesNoIssues() throws Exception {
        String html = """
                <html><head>
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta property="og:title" content="T"><meta property="og:description" content="D">
                  <meta property="og:image" content="https://example.com/i.png">
                  <meta name="twitter:card" content="summary">
                  <link rel="canonical" href="https://example.com/">
                  <link rel="alternate" hreflang="en" href="https://example.com/">
                  <link rel="alternate" hreflang="x-default" href="https://example.com/">
                  <script type="application/ld+json">{"@context":"https://schema.org","@type":"Organization"}</script>
                </head><body><a href="/about">About</a><img src="https://example.com/a.png"></body></html>
                """;

        AuditPage page = page(html, 0);

        assertThat(page.getSignals().jsonLdTypes()).containsExactly("Organization");
        assertThat(page.getSignals().internalLinks()).contains("https://example.com/about");
        assertThat(ruleCodes(page)).isEmpty();
    }

    @Test
    void bareHomepageReportsMissingSignals() throws Exception {
        AuditPage page = page("<html><head><title>x</title></head><body></body></html>", 0);

        assertThat(ruleCodes(page)).containsExactlyInAnyOrder(
                "OPEN_GRAPH_INCOMPLETE",
                "TWITTER_CARD_MISSING",
                "STRUCTURED_DATA_MISSING",
                "VIEWPORT_MISSING"
        );
    }

    @Test
    void problemsInTheMarkupAreDetected() throws Exception {
        String html = """
                <html><head>
                  <meta name="viewport" content="width=device-width, user-scalable=no">
                  <meta name="robots" content="noindex, follow">
                  <link rel="canonical" href="https://example.com/other">
                  <link rel="alternate" hreflang="english" href="https://example.com/en">
                  <script type="application/ld+json">{not json</script>
                </head><body><img src="http://cdn.example.com/a.png"></body></html>
                """;

        assertThat(ruleCodes(page(html, 1))).contains(
                "VIEWPORT_INVALID",
                "NOINDEX",
                "CANONICAL_POINTS_ELSEWHERE",
                "HREFLANG_INVALID",
                "STRUCTURED_DATA_INVALID",
                "MIXED_CONTENT"
        );
    }

    @Test
    void xRobotsTagHeaderCanBlockIndexing() {
        assertThat(IndexabilityAnalyzer.blocksIndexing("googlebot: noindex")).isTrue();
        assertThat(IndexabilityAnalyzer.blocksIndexing("none")).isTrue();
        assertThat(IndexabilityAnalyzer.blocksIndexing("index, follow")).isFalse();
        assertThat(IndexabilityAnalyzer.blocksIndexing(null)).isFalse();
    }

    @Test
    void siteAnalyzersFindDuplicatesBrokenLinksAndOrphans() {
        AuditPage home = crawled(1L, "https://example.com/", 0, 200, "Home",
                List.of("https://example.com/a", "https://example.com/missing"), false);
        AuditPage a = crawled(2L, "https://example.com/a", 1, 200, "Same title", List.of(), false);
        AuditPage b = crawled(3L, "https://example.com/b", 1, 200, "same  TITLE", List.of(), true);
        AuditPage missing = crawled(4L, "https://example.com/missing", 1, 404, null, List.of(), false);
        SiteContext context = new SiteContext(List.of(home, a, b, missing), Map.of());

        assertThat(rules(new DuplicateTitleAnalyzer().analyze(context))).containsExactly("DUPLICATE_TITLE", "DUPLICATE_TITLE");
        assertThat(new BrokenLinkAnalyzer().analyze(context))
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.page()).isSameAs(home);
                    assertThat(issue.result().ruleCode()).isEqualTo("BROKEN_INTERNAL_LINK");
                });
        assertThat(new OrphanPageAnalyzer().analyze(context))
                .singleElement()
                .satisfies(issue -> assertThat(issue.page()).isSameAs(b));
    }

    private AuditPage page(String html, int depth) throws Exception {
        Document document = PageExtractor.parse(html.getBytes(StandardCharsets.UTF_8), "UTF-8", URL);
        PageExtractor.PageContent content = extractor.extract(document, new SiteScope("example.com")::contains);
        return AuditPage.builder()
                .url(URL)
                .statusCode(200)
                .status(AuditPageStatus.CRAWLED)
                .depth(depth)
                .canonicalUrl(content.canonicalUrl())
                .signals(content.signals().withResponse(null, html.length(), null))
                .build();
    }

    private static AuditPage crawled(Long id, String url, int depth, int status, String title, List<String> links, boolean inSitemap) {
        PageSignals signals = new PageSignals(null, null, null, null, null, null, null, null, null, null,
                null, null, null, links, List.of(), inSitemap, null, null, null);
        return AuditPage.builder()
                .id(id)
                .url(url)
                .depth(depth)
                .status(AuditPageStatus.CRAWLED)
                .statusCode(status)
                .title(title)
                .signals(signals)
                .build();
    }

    private List<String> ruleCodes(AuditPage page) {
        return analyzers.stream()
                .flatMap(analyzer -> analyzer.analyze(page).stream())
                .map(SeoIssueResult::ruleCode)
                .toList();
    }

    private static List<String> rules(List<SiteIssue> issues) {
        return issues.stream().map(issue -> issue.result().ruleCode()).toList();
    }
}
