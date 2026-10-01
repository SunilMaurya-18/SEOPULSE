package com.seopulse.website.seo.analyzer.site;

import java.util.List;

/**
 * Checks that need every page of an audit (duplicates, link graph,
 * site-wide headers). Runs once after the page-level analyzers.
 */
public interface SiteAnalyzer {

    String getName();

    List<SiteIssue> analyze(SiteContext context);
}
