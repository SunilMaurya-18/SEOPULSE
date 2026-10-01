package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.seo.model.IssueRow;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleCategory;
import com.seopulse.website.seo.rules.RuleDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scoring version 2. Each page starts at 100 and loses the rule weight of
 * every issue it has; the audit score is the average page score weighted
 * by page importance, so problems on the homepage and well-linked pages
 * count more than problems deep in an archive. Category scores apply the
 * same formula to one category's issues at a time.
 */
@Service
@RequiredArgsConstructor
public class SeoScoreService {

    public static final int SCORE_VERSION = 2;

    private static final int MAX_SCORE = 100;

    private final RuleCatalog ruleCatalog;

    public record ScoreResult(
            int score,
            Map<String, Integer> categoryScores,
            int issueCount,
            int errorCount,
            int warningCount,
            int infoCount
    ) {
    }

    /**
     * @param pages        every page of the audit
     * @param issues       every issue of the audit
     * @param inboundLinks number of other crawled pages linking to each URL
     */
    public ScoreResult score(List<AuditPage> pages, List<IssueRow> issues, Map<String, Integer> inboundLinks) {

        Map<Long, List<IssueRow>> issuesByPage = issues.stream()
                .collect(Collectors.groupingBy(IssueRow::pageId));

        Set<Long> pagesWithIssues = issuesByPage.keySet();
        List<AuditPage> scored = new ArrayList<>();
        for (AuditPage page : pages) {
            if (page.getStatus() == AuditPageStatus.CRAWLED || pagesWithIssues.contains(page.getId())) {
                scored.add(page);
            }
        }

        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (IssueRow issue : issues) {
            String severity = issue.severity() == null ? "" : issue.severity().toUpperCase();
            switch (severity) {
                case "ERROR" -> errors++;
                case "WARNING" -> warnings++;
                case "INFO" -> infos++;
                default -> {
                }
            }
        }

        if (scored.isEmpty()) {
            return new ScoreResult(0, Map.of(), issues.size(), errors, warnings, infos);
        }

        double totalWeight = 0;
        double weightedScore = 0;
        Map<RuleCategory, Double> weightedCategory = new EnumMap<>(RuleCategory.class);

        for (AuditPage page : scored) {
            double importance = importance(page, inboundLinks.getOrDefault(page.getUrl(), 0));
            List<IssueRow> pageIssues = issuesByPage.getOrDefault(page.getId(), List.of());

            int deduction = 0;
            Map<RuleCategory, Integer> categoryDeduction = new EnumMap<>(RuleCategory.class);
            for (IssueRow issue : pageIssues) {
                RuleDefinition rule = ruleCatalog.get(issue.ruleCode(), issue.severity());
                deduction += rule.weight();
                categoryDeduction.merge(category(issue, rule), rule.weight(), Integer::sum);
            }

            totalWeight += importance;
            weightedScore += importance * Math.max(0, MAX_SCORE - deduction);

            for (RuleCategory category : RuleCategory.values()) {
                int categoryScore = Math.max(0, MAX_SCORE - categoryDeduction.getOrDefault(category, 0));
                weightedCategory.merge(category, importance * categoryScore, Double::sum);
            }
        }

        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        for (RuleCategory category : RuleCategory.values()) {
            categoryScores.put(category.name(), (int) Math.round(weightedCategory.get(category) / totalWeight));
        }

        return new ScoreResult(
                (int) Math.round(weightedScore / totalWeight),
                categoryScores,
                issues.size(),
                errors,
                warnings,
                infos
        );
    }

    /**
     * Homepage 3x, depth 1 2x, falling towards 1x with depth; up to a further
     * 2x for pages with many inbound internal links.
     */
    static double importance(AuditPage page, int inboundLinks) {
        int depth = page.getDepth() == null ? 0 : Math.max(0, page.getDepth());
        double depthFactor = 1 + 2.0 / (1 + depth);
        double linkFactor = 1 + Math.min(1.0, inboundLinks / 20.0);
        return depthFactor * linkFactor;
    }

    private static RuleCategory category(IssueRow issue, RuleDefinition rule) {
        if (issue.category() != null) {
            try {
                return RuleCategory.valueOf(issue.category());
            } catch (IllegalArgumentException ignored) {
                // fall through to the catalog category
            }
        }
        return rule.category();
    }
}
