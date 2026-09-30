package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.analyzer.SeoAnalyzer;
import com.seopulse.website.seo.entity.SeoIssue;
import com.seopulse.website.seo.model.SeoIssueResult;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeoAnalysisService {

    private final List<SeoAnalyzer> analyzers;
    private final SeoIssueRepository seoIssueRepository;

    public SeoAnalysisService(
            List<SeoAnalyzer> analyzers,
            SeoIssueRepository seoIssueRepository
    ) {
        this.analyzers = analyzers;
        this.seoIssueRepository = seoIssueRepository;
    }

    @Transactional
    public List<SeoIssue> analyzeAndSave(
            AuditPage page
    ) {

        seoIssueRepository.deleteByAuditPageId(page.getId());
        List<SeoIssue> savedIssues =
                new ArrayList<>();

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

                SeoIssue issue =
                        SeoIssue.builder()
                                .auditPage(page)
                                .ruleCode(result.ruleCode())
                                .severity(result.severity())
                                .message(result.message())
                                .recommendations(
                                        buildRecommendation(
                                                result.ruleCode()
                                        )
                                )
                                .build();

                SeoIssue saved =
                        seoIssueRepository.save(issue);

                savedIssues.add(saved);
            }
        }

        log.info(
                "SEO analysis completed: page={}, issues={}",
                page.getUrl(),
                savedIssues.size()
        );

        return savedIssues;
    }

    private String buildRecommendation(
            String ruleCode
    ) {

        return switch (ruleCode) {

            case "TITLE_MISSING" ->
                    "Add a unique and descriptive title to the page.";

            case "TITLE_TOO_SHORT" ->
                    "Make the page title more descriptive.";

            case "TITLE_TOO_LONG" ->
                    "Shorten the page title.";

            case "META_DESCRIPTION_MISSING" ->
                    "Add a unique meta description.";

            case "META_DESCRIPTION_TOO_SHORT" ->
                    "Expand the meta description to provide useful page context.";

            case "META_DESCRIPTION_TOO_LONG" ->
                    "Shorten the meta description.";

            case "H1_MISSING" ->
                    "Add a clear primary H1 heading.";

            case "MULTIPLE_H1" ->
                    "Review the page structure and keep a clear primary H1.";

            case "CANONICAL_MISSING" ->
                    "Add a canonical URL when appropriate.";

            case "HTTP_4XX" ->
                    "Fix the broken or inaccessible page.";

            case "HTTP_5XX" ->
                    "Investigate the server error.";

            case "LOW_WORD_COUNT" ->
                    "Review whether the page provides sufficient useful content.";

            case "IMAGE_ALT_MISSING" ->
                    "Add meaningful alt text to informative images.";

            case "NO_INTERNAL_LINKS" ->
                    "Add relevant internal links to help users and search engines discover related content.";

            default ->
                    "Review this SEO issue and make the recommended improvement.";
        };
    }
}