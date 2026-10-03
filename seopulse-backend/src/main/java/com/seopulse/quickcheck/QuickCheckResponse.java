package com.seopulse.quickcheck;

import java.util.List;
import java.util.Map;

public record QuickCheckResponse(
        String url,
        int score,
        Map<String, Integer> categoryScores,
        int pagesChecked,
        boolean partial,
        int errorCount,
        int warningCount,
        int infoCount,
        int issueTypes,
        List<Issue> issues
) {

    public record Issue(
            String ruleCode,
            String title,
            String severity,
            String category,
            String recommendation,
            String helpUrl,
            int pages
    ) {
    }
}
