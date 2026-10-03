package com.seopulse.quickcheck;

import com.seopulse.common.exception.RateLimitExceededException;
import com.seopulse.website.crawler.CrawlResult;
import com.seopulse.website.crawler.CrawledPage;
import com.seopulse.website.crawler.WebsiteCrawler;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.seo.analyzer.SeoAnalyzer;
import com.seopulse.website.seo.analyzer.site.SiteAnalyzer;
import com.seopulse.website.seo.analyzer.site.SiteContext;
import com.seopulse.website.seo.analyzer.site.SiteIssue;
import com.seopulse.website.seo.model.IssueRow;
import com.seopulse.website.seo.model.SeoIssueResult;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleDefinition;
import com.seopulse.website.seo.service.SeoScoreService;
import com.seopulse.website.service.AuditCrawlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/**
 * A short, unsaved audit for visitors who have not signed up: a few pages
 * are crawled within a small time budget and run through the same
 * analyzers and scoring as a full audit. External links are not checked.
 */
@Service
@Slf4j
public class QuickCheckService {

    private static final Pattern HAS_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    private static final List<String> SEVERITY_ORDER = List.of("ERROR", "WARNING", "INFO");

    private final WebsiteCrawler crawler;
    private final List<SeoAnalyzer> analyzers;
    private final List<SiteAnalyzer> siteAnalyzers;
    private final SeoScoreService scoreService;
    private final RuleCatalog ruleCatalog;
    private final QuickCheckProperties properties;
    private final Semaphore running;

    public QuickCheckService(
            WebsiteCrawler crawler,
            List<SeoAnalyzer> analyzers,
            List<SiteAnalyzer> siteAnalyzers,
            SeoScoreService scoreService,
            RuleCatalog ruleCatalog,
            QuickCheckProperties properties
    ) {
        this.crawler = crawler;
        this.analyzers = analyzers;
        this.siteAnalyzers = siteAnalyzers;
        this.scoreService = scoreService;
        this.ruleCatalog = ruleCatalog;
        this.properties = properties;
        this.running = new Semaphore(Math.max(1, properties.getMaxConcurrent()));
    }

    /** Adds {@code https://} when the visitor typed a bare domain. */
    public static String normalize(String rawUrl) {
        String trimmed = rawUrl == null ? "" : rawUrl.trim();
        if (trimmed.isEmpty() || HAS_SCHEME.matcher(trimmed).matches()) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    /**
     * @throws IllegalArgumentException if the URL is not allowed or the homepage cannot be checked
     * @throws RateLimitExceededException if too many checks are already running
     */
    public QuickCheckResponse check(String rawUrl) {
        if (!running.tryAcquire()) {
            throw new RateLimitExceededException(properties.getMaxConcurrent(), 15);
        }
        try {
            return run(normalize(rawUrl));
        } finally {
            running.release();
        }
    }

    private QuickCheckResponse run(String url) {

        CrawlResult result;
        try {
            result = crawler.crawl(url, properties.getMaxPages(), properties.getBudget());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Quick check interrupted", ex);
        }

        List<AuditPage> pages = toPages(result);
        AuditPage homepage = pages.stream()
                .filter(page -> page.getStatus() == AuditPageStatus.CRAWLED && page.getDepth() == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(unreachableReason(result)));

        Integer status = homepage.getStatusCode();
        if (status == null || status < 200 || status >= 300) {
            throw new IllegalArgumentException(
                    "The homepage returned HTTP " + status + ", so there is nothing to check yet.");
        }

        SiteContext context = new SiteContext(pages, Map.of());
        List<SiteIssue> findings = analyze(context);

        List<IssueRow> rows = findings.stream()
                .map(finding -> {
                    SeoIssueResult issue = finding.result();
                    RuleDefinition rule = ruleCatalog.get(issue.ruleCode(), issue.severity());
                    return new IssueRow(finding.page().getId(), issue.ruleCode(), issue.severity(),
                            rule.category().name());
                })
                .toList();

        SeoScoreService.ScoreResult score = scoreService.score(pages, rows, context.inboundLinkCounts());
        List<QuickCheckResponse.Issue> issues = groupByRule(findings);

        log.info("Quick check finished: url={}, pages={}, score={}, issueTypes={}",
                homepage.getUrl(), context.crawledPages().size(), score.score(), issues.size());

        return new QuickCheckResponse(
                homepage.getUrl(),
                score.score(),
                score.categoryScores(),
                context.crawledPages().size(),
                result.timedOut(),
                score.errorCount(),
                score.warningCount(),
                score.infoCount(),
                issues.size(),
                issues.stream().limit(properties.getMaxIssues()).toList()
        );
    }

    /** Unsaved pages get temporary ids, which scoring and site analyzers use as keys. */
    private static List<AuditPage> toPages(CrawlResult result) {
        Instant now = Instant.now();
        Set<String> seen = new HashSet<>();
        List<AuditPage> pages = new ArrayList<>();
        long id = 1;
        for (CrawledPage crawled : result.pages()) {
            if (!seen.add(crawled.url())) {
                continue;
            }
            AuditPage page = AuditCrawlerService.toAuditPage(null, crawled, result, now);
            page.setId(id++);
            pages.add(page);
        }
        return pages;
    }

    private List<SiteIssue> analyze(SiteContext context) {
        Set<String> keys = new HashSet<>();
        List<SiteIssue> findings = new ArrayList<>();

        for (AuditPage page : context.crawledPages()) {
            for (SeoAnalyzer analyzer : analyzers) {
                List<SeoIssueResult> results = analyzer.analyze(page);
                if (results == null) {
                    continue;
                }
                for (SeoIssueResult issue : results) {
                    if (keys.add(page.getId() + "|" + issue.ruleCode())) {
                        findings.add(new SiteIssue(page, issue));
                    }
                }
            }
        }

        for (SiteAnalyzer analyzer : siteAnalyzers) {
            try {
                for (SiteIssue issue : analyzer.analyze(context)) {
                    if (keys.add(issue.page().getId() + "|" + issue.result().ruleCode())) {
                        findings.add(issue);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("Site analyzer failed during quick check: analyzer={}", analyzer.getName(), ex);
            }
        }

        return findings;
    }

    private List<QuickCheckResponse.Issue> groupByRule(List<SiteIssue> findings) {
        Map<String, List<SiteIssue>> byRule = new LinkedHashMap<>();
        for (SiteIssue finding : findings) {
            byRule.computeIfAbsent(finding.result().ruleCode(), code -> new ArrayList<>()).add(finding);
        }

        List<RankedIssue> ranked = new ArrayList<>();
        for (List<SiteIssue> group : byRule.values()) {
            String severity = group.stream()
                    .map(finding -> finding.result().severity())
                    .min(Comparator.comparingInt(QuickCheckService::severityRank))
                    .orElse("INFO");
            String code = group.getFirst().result().ruleCode();
            RuleDefinition rule = ruleCatalog.get(code, severity);
            int pageCount = (int) group.stream().map(finding -> finding.page().getId()).distinct().count();
            ranked.add(new RankedIssue(new QuickCheckResponse.Issue(
                    code,
                    rule.title(),
                    severity,
                    rule.category().name(),
                    rule.recommendation(),
                    rule.helpUrl(),
                    pageCount
            ), rule.weight()));
        }

        return ranked.stream()
                .sorted(Comparator.comparingInt((RankedIssue item) -> severityRank(item.issue().severity()))
                        .thenComparing(Comparator.comparingInt(RankedIssue::weight).reversed())
                        .thenComparing(Comparator.comparingInt((RankedIssue item) -> item.issue().pages()).reversed()))
                .map(RankedIssue::issue)
                .toList();
    }

    private record RankedIssue(QuickCheckResponse.Issue issue, int weight) {
    }

    private static int severityRank(String severity) {
        int index = SEVERITY_ORDER.indexOf(severity == null ? "" : severity.toUpperCase());
        return index < 0 ? SEVERITY_ORDER.size() : index;
    }

    private static String unreachableReason(CrawlResult result) {
        if (result.pages().isEmpty()) {
            return "We could not load a web page from this address. Check it and try again.";
        }
        CrawledPage first = result.pages().getFirst();
        return switch (first.outcome()) {
            case SKIPPED_ROBOTS -> "This site's robots.txt does not allow SEOPulseBot to check it.";
            case REDIRECT -> first.skipReason() != null
                    ? "The homepage could not be followed: " + first.skipReason() + "."
                    : "The homepage redirect could not be followed.";
            case TOO_LARGE -> "The homepage is too large to check.";
            case FAILED, CRAWLED -> "We could not reach this website. Check the address and try again.";
        };
    }
}
