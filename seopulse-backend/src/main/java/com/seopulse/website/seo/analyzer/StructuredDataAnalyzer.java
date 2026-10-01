package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructuredDataAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Structured Data Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null || !OpenGraphAnalyzer.isSuccess(page)) {
            return List.of();
        }

        int blocks = signals.jsonLdBlocks() == null ? 0 : signals.jsonLdBlocks();
        int invalid = signals.jsonLdInvalid() == null ? 0 : signals.jsonLdInvalid();

        if (invalid > 0) {
            return List.of(new SeoIssueResult(
                    "STRUCTURED_DATA_INVALID",
                    "WARNING",
                    invalid == 1
                            ? "One JSON-LD block could not be parsed or has no @type"
                            : invalid + " JSON-LD blocks could not be parsed or have no @type"
            ));
        }

        // Only the homepage: flagging every page without markup would be noise.
        if (blocks == 0 && page.getDepth() != null && page.getDepth() == 0) {
            return List.of(new SeoIssueResult(
                    "STRUCTURED_DATA_MISSING",
                    "INFO",
                    "Homepage has no JSON-LD structured data"
            ));
        }

        return List.of();
    }
}
