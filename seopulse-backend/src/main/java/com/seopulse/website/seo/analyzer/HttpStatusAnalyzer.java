package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HttpStatusAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "HTTP Status Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        int statusCode =
                page.getStatusCode();

        if (statusCode >= 400
                && statusCode < 500) {

            issues.add(new SeoIssueResult(
                    "HTTP_4XX",
                    "ERROR",
                    "Page returns a 4xx HTTP status"
            ));
        }

        if (statusCode >= 500) {

            issues.add(new SeoIssueResult(
                    "HTTP_5XX",
                    "ERROR",
                    "Page returns a 5xx HTTP status"
            ));
        }

        return issues;
    }
}