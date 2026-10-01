package com.seopulse.website.seo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.analyzer.site.ExternalLinkChecker;
import com.seopulse.website.seo.analyzer.site.SiteAnalyzer;
import com.seopulse.website.seo.analyzer.site.SiteContext;
import com.seopulse.website.seo.analyzer.site.SiteIssue;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.service.AuditStateChangedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuditAnalysisService {

    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final SeoAnalysisService seoAnalysisService;
    private final SeoScoreService seoScoreService;
    private final List<SiteAnalyzer> siteAnalyzers;
    private final ExternalLinkChecker externalLinkChecker;
    private final ObjectMapper objectMapper;

    public AuditAnalysisService(
            AuditRepository auditRepository,
            AuditPageRepository auditPageRepository,
            SeoIssueRepository seoIssueRepository,
            SeoAnalysisService seoAnalysisService,
            SeoScoreService seoScoreService,
            List<SiteAnalyzer> siteAnalyzers,
            ExternalLinkChecker externalLinkChecker,
            ObjectMapper objectMapper
    ) {
        this.auditRepository = auditRepository;
        this.auditPageRepository = auditPageRepository;
        this.seoIssueRepository = seoIssueRepository;
        this.seoAnalysisService = seoAnalysisService;
        this.seoScoreService = seoScoreService;
        this.siteAnalyzers = siteAnalyzers;
        this.externalLinkChecker = externalLinkChecker;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyzes crawled pages and moves the audit from ANALYZING to
     * COMPLETED. Failures propagate; the worker decides between retry
     * and FAILED.
     *
     * @throws AuditStateChangedException if the audit was cancelled meanwhile
     */
    public void analyzeAudit(Long auditId) {

        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.ANALYZING) {
            throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
        }

        log.info("Starting SEO analysis: auditId={}", auditId);

        List<AuditPage> pages = auditPageRepository.findByAuditId(auditId);
        List<AuditPage> crawledPages = pages.stream()
                .filter(page -> page.getStatus() == AuditPageStatus.CRAWLED)
                .toList();

        for (AuditPage page : crawledPages) {
            checkInterrupted(auditId);
            seoAnalysisService.analyzeAndSave(page);
        }

        checkInterrupted(auditId);
        SiteContext context = new SiteContext(pages, externalLinkChecker.check(crawledPages));
        runSiteAnalyzers(auditId, context);

        checkInterrupted(auditId);
        SeoScoreService.ScoreResult result = seoScoreService.score(
                pages,
                seoIssueRepository.findRowsByAuditId(auditId),
                context.inboundLinkCounts()
        );

        int updated = auditRepository.completeAnalysis(
                auditId,
                AuditStatus.ANALYZING.name(),
                AuditStatus.COMPLETED.name(),
                result.score(),
                crawledPages.size(),
                Instant.now(),
                result.issueCount(),
                result.errorCount(),
                result.warningCount(),
                result.infoCount(),
                toJson(result.categoryScores()),
                SeoScoreService.SCORE_VERSION
        );

        if (updated == 0) {
            throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
        }

        log.info(
                "SEO analysis completed: auditId={}, pagesAnalyzed={}, score={}, issues={}",
                auditId,
                crawledPages.size(),
                result.score(),
                result.issueCount()
        );
    }

    private void runSiteAnalyzers(Long auditId, SiteContext context) {
        List<SiteIssue> issues = new ArrayList<>();
        for (SiteAnalyzer analyzer : siteAnalyzers) {
            try {
                issues.addAll(analyzer.analyze(context));
            } catch (RuntimeException ex) {
                // One faulty check should not fail the whole audit.
                log.warn("Site analyzer failed: analyzer={}, auditId={}", analyzer.getName(), auditId, ex);
            }
        }
        int saved = seoAnalysisService.saveSiteIssues(auditId, issues);
        log.debug("Site analysis saved {} issues: auditId={}", saved, auditId);
    }

    private static void checkInterrupted(Long auditId) {
        if (Thread.currentThread().isInterrupted()) {
            throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
        }
    }

    private String toJson(Map<String, Integer> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
