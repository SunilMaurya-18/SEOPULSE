package com.seopulse.report.render;

import com.seopulse.organization.entity.Organization;
import com.seopulse.organization.entity.OrganizationBranding;
import com.seopulse.website.comparison.AuditComparison;
import com.seopulse.website.comparison.AuditComparisonService;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.seo.model.IssueSnapshot;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportModelFactory {

    static final int MAX_RULE_GROUPS = 30;
    static final int MAX_SAMPLE_URLS = 5;

    private final SeoIssueRepository seoIssueRepository;
    private final AuditComparisonService comparisonService;
    private final RuleCatalog ruleCatalog;

    @Transactional(readOnly = true)
    public ReportModel build(Audit audit, boolean whiteLabel, boolean watermark) {

        Organization organization = audit.getWebsite().getProject().getOrganization();
        boolean detailsAvailable = audit.getDetailsPurgedAt() == null;
        List<IssueSnapshot> issues = detailsAvailable ? seoIssueRepository.findSnapshotsByAuditId(audit.getId()) : List.of();

        AuditComparison comparison = comparisonService.compareWithPrevious(audit.getId());
        boolean comparable = comparison.baseline() != null && comparison.detailsAvailable();

        int errors = audit.getErrorCount() != null ? audit.getErrorCount() : count(issues, "ERROR");
        int warnings = audit.getWarningCount() != null ? audit.getWarningCount() : count(issues, "WARNING");
        int infos = audit.getInfoCount() != null ? audit.getInfoCount() : count(issues, "INFO");

        return new ReportModel(
                audit.getId(),
                audit.getWebsite().getName(),
                audit.getWebsite().getUrl(),
                audit.getCompletedAt(),
                audit.getScore(),
                audit.getScoreVersion(),
                audit.getCategoryScores() == null ? Map.of() : audit.getCategoryScores(),
                audit.getPagesCrawled() == null ? 0 : audit.getPagesCrawled(),
                audit.getIssueCount() != null ? audit.getIssueCount() : issues.size(),
                errors,
                warnings,
                infos,
                comparison.baseline() == null ? null : comparison.baseline().getScore(),
                comparable ? comparison.newIssues().size() : null,
                comparable ? comparison.fixedIssues().size() : null,
                detailsAvailable,
                group(issues),
                whiteLabel ? branding(organization.getBranding()) : null,
                watermark
        );
    }

    private List<ReportModel.RuleGroup> group(List<IssueSnapshot> issues) {

        Map<String, List<IssueSnapshot>> byRule = new LinkedHashMap<>();
        for (IssueSnapshot issue : issues) {
            byRule.computeIfAbsent(issue.ruleCode(), key -> new ArrayList<>()).add(issue);
        }

        return byRule.entrySet().stream()
                .map(entry -> {
                    IssueSnapshot first = entry.getValue().getFirst();
                    RuleDefinition rule = ruleCatalog.get(entry.getKey(), first.severity());
                    return new ReportModel.RuleGroup(
                            entry.getKey(),
                            rule.title(),
                            first.severity(),
                            first.category() != null ? first.category() : rule.category().name(),
                            entry.getValue().size(),
                            rule.recommendation(),
                            rule.helpUrl(),
                            entry.getValue().stream().map(IssueSnapshot::url).distinct().limit(MAX_SAMPLE_URLS).toList()
                    );
                })
                .sorted(Comparator.comparingInt((ReportModel.RuleGroup group) -> severityRank(group.severity()))
                        .thenComparing(Comparator.comparingInt(ReportModel.RuleGroup::count).reversed()))
                .limit(MAX_RULE_GROUPS)
                .toList();
    }

    private static ReportModel.Branding branding(OrganizationBranding branding) {
        if (branding == null) {
            return null;
        }
        return new ReportModel.Branding(branding.companyName(), branding.brandColor(), branding.coverText(), branding.logoDataUrl());
    }

    private static int count(List<IssueSnapshot> issues, String severity) {
        return (int) issues.stream().filter(issue -> severity.equalsIgnoreCase(issue.severity())).count();
    }

    private static int severityRank(String severity) {
        if (severity == null) {
            return 3;
        }
        return switch (severity.toUpperCase()) {
            case "ERROR" -> 0;
            case "WARNING" -> 1;
            default -> 2;
        };
    }
}
