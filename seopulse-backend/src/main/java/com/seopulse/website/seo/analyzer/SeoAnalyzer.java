package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;

import java.util.List;

public interface SeoAnalyzer {

    String getName();

    List<SeoIssueResult> analyze(AuditPage page);
}