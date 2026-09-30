package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Image Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        if (page.getImagesWithoutAlt() > 0) {

            issues.add(new SeoIssueResult(
                    "IMAGE_ALT_MISSING",
                    "WARNING",
                    "One or more images are missing alt text"
            ));
        }

        return issues;
    }
}