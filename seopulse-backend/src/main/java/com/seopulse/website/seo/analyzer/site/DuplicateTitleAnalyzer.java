package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class DuplicateTitleAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Duplicate Title Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {
        return Duplicates.find(context.successfulPages(), AuditPage::getTitle, "DUPLICATE_TITLE", "title");
    }

    static final class Duplicates {

        private Duplicates() {
        }

        static List<SiteIssue> find(
                List<AuditPage> pages,
                Function<AuditPage, String> value,
                String ruleCode,
                String label
        ) {
            var groups = pages.stream()
                    .filter(page -> value.apply(page) != null && !value.apply(page).isBlank())
                    .collect(java.util.stream.Collectors.groupingBy(
                            page -> value.apply(page).trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT)));

            return groups.values().stream()
                    .filter(group -> group.size() > 1)
                    .flatMap(group -> group.stream().map(page -> SiteIssue.of(
                            page,
                            ruleCode,
                            "WARNING",
                            "Same " + label + " as " + (group.size() - 1)
                                    + (group.size() == 2 ? " other page" : " other pages")
                                    + ", e.g. " + firstOther(group, page)
                    )))
                    .toList();
        }

        private static String firstOther(List<AuditPage> group, AuditPage page) {
            return group.stream()
                    .filter(other -> other != page)
                    .map(AuditPage::getUrl)
                    .findFirst()
                    .orElse("");
        }
    }
}
