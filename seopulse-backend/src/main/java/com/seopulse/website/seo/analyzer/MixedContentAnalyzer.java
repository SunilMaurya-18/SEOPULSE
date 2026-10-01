package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MixedContentAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Mixed Content Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null || signals.mixedContent() == null || signals.mixedContent() == 0) {
            return List.of();
        }

        int count = signals.mixedContent();
        String sample = signals.mixedContentSamples() == null || signals.mixedContentSamples().isEmpty()
                ? ""
                : ", e.g. " + signals.mixedContentSamples().getFirst();

        return List.of(new SeoIssueResult(
                "MIXED_CONTENT",
                "ERROR",
                truncate((count == 1 ? "1 resource is" : count + " resources are") + " loaded over HTTP" + sample)
        ));
    }

    private static String truncate(String message) {
        return message.length() > 500 ? message.substring(0, 497) + "..." : message;
    }
}
