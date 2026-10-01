package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.analyzer.SeoAnalyzer;
import com.seopulse.website.seo.analyzer.site.SiteIssue;
import com.seopulse.website.seo.entity.SeoIssue;
import com.seopulse.website.seo.model.SeoIssueResult;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.seo.rules.IssueFingerprint;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SeoAnalysisService {

    private final List<SeoAnalyzer> analyzers;
    private final SeoIssueRepository seoIssueRepository;
    private final RuleCatalog ruleCatalog;

    public SeoAnalysisService(
            List<SeoAnalyzer> analyzers,
            SeoIssueRepository seoIssueRepository,
            RuleCatalog ruleCatalog
    ) {
        this.analyzers = analyzers;
        this.seoIssueRepository = seoIssueRepository;
        this.ruleCatalog = ruleCatalog;
    }

    @Transactional
    public List<SeoIssue> analyzeAndSave(
            AuditPage page
    ) {

        seoIssueRepository.deleteByAuditPageId(page.getId());
        List<SeoIssue> savedIssues =
                new ArrayList<>();
        Set<String> seenRules = new HashSet<>();

        for (SeoAnalyzer analyzer : analyzers) {

            log.debug(
                    "Running analyzer: {} for page {}",
                    analyzer.getName(),
                    page.getUrl()
            );

            List<SeoIssueResult> results =
                    analyzer.analyze(page);

            if (results == null || results.isEmpty()) {
                continue;
            }

            for (SeoIssueResult result : results) {
                if (seenRules.add(result.ruleCode())) {
                    savedIssues.add(seoIssueRepository.save(toEntity(page, result)));
                }
            }
        }

        log.debug(
                "SEO analysis completed: page={}, issues={}",
                page.getUrl(),
                savedIssues.size()
        );

        return savedIssues;
    }

    /**
     * Saves site-level findings. A page has at most one issue per rule, so
     * findings for a rule the page already has are skipped.
     */
    @Transactional
    public int saveSiteIssues(Long auditId, List<SiteIssue> issues) {

        Set<String> existing = new HashSet<>(seoIssueRepository.findPageRuleKeysByAuditId(auditId));
        int saved = 0;

        for (SiteIssue issue : issues) {
            String key = issue.page().getId() + "|" + issue.result().ruleCode();
            if (existing.add(key)) {
                seoIssueRepository.save(toEntity(issue.page(), issue.result()));
                saved++;
            }
        }

        return saved;
    }

    private SeoIssue toEntity(AuditPage page, SeoIssueResult result) {
        RuleDefinition rule = ruleCatalog.get(result.ruleCode(), result.severity());
        return SeoIssue.builder()
                .auditPage(page)
                .ruleCode(result.ruleCode())
                .severity(result.severity())
                .message(result.message())
                .recommendations(rule.recommendation())
                .category(rule.category().name())
                .fingerprint(IssueFingerprint.of(result.ruleCode(), page.getUrl()))
                .build();
    }
}
