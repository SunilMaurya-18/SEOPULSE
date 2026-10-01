package com.seopulse.website.seo.rules;

import com.seopulse.auth.service.SecureTokens;

/**
 * Identifies "the same issue" across audits: the rule plus the page URL,
 * ignoring scheme, a leading www. and trailing slashes so that an
 * http-to-https move does not mark every issue as new.
 */
public final class IssueFingerprint {

    private IssueFingerprint() {
    }

    // Must stay identical to the backfill in V9__issue_fingerprints_and_rules.sql.
    public static String of(String ruleCode, String url) {
        return SecureTokens.sha256Hex(ruleCode + "|" + urlKey(url));
    }

    static String urlKey(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceFirst("(?i)^https?://(www\\.)?", "").replaceFirst("/+$", "");
    }
}
