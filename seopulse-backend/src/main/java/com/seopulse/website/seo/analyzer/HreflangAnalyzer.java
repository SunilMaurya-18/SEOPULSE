package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks hreflang values on one page. Missing return links need the other
 * pages and are checked by {@code HreflangReturnLinkAnalyzer}.
 */
@Component
public class HreflangAnalyzer implements SeoAnalyzer {

    // language, optional script, optional region (en, en-GB, zh-Hant-TW)
    private static final Pattern HREFLANG = Pattern.compile("^[a-z]{2,3}(-[a-z]{4})?(-([a-z]{2}|\\d{3}))?$");

    private static final Set<String> ISO_639_1 = Set.of(Locale.getISOLanguages());

    @Override
    public String getName() {
        return "Hreflang Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null) {
            return List.of();
        }

        List<String> invalid = signals.hreflangOrEmpty().stream()
                .filter(entry -> !isValid(entry.lang()) || entry.href() == null)
                .map(entry -> entry.lang() == null || entry.lang().isBlank() ? "(empty)" : entry.lang())
                .distinct()
                .toList();

        if (invalid.isEmpty()) {
            return List.of();
        }

        String message = "Invalid hreflang value" + (invalid.size() == 1 ? ": " : "s: ")
                + String.join(", ", invalid.stream().limit(10).toList());

        return List.of(new SeoIssueResult(
                "HREFLANG_INVALID",
                "WARNING",
                message.length() > 500 ? message.substring(0, 497) + "..." : message
        ));
    }

    static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.equals("x-default")) {
            return true;
        }
        if (!HREFLANG.matcher(normalized).matches()) {
            return false;
        }
        String language = normalized.split("-")[0];
        return language.length() == 3 || ISO_639_1.contains(language);
    }
}
