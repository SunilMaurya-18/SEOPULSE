package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ViewportAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Viewport Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null || !OpenGraphAnalyzer.isSuccess(page)) {
            return List.of();
        }

        String viewport = signals.viewport();

        if (viewport == null) {
            return List.of(new SeoIssueResult(
                    "VIEWPORT_MISSING",
                    "ERROR",
                    "Page has no viewport meta tag, so mobile browsers render it at desktop width"
            ));
        }

        String normalized = viewport.toLowerCase(Locale.ROOT).replace(" ", "");

        if (!normalized.contains("width=device-width")) {
            return List.of(new SeoIssueResult(
                    "VIEWPORT_INVALID",
                    "WARNING",
                    "Viewport does not set width=device-width"
            ));
        }

        if (normalized.contains("user-scalable=no") || normalized.contains("user-scalable=0")) {
            return List.of(new SeoIssueResult(
                    "VIEWPORT_INVALID",
                    "WARNING",
                    "Viewport disables zooming (user-scalable=no)"
            ));
        }

        return List.of();
    }
}
