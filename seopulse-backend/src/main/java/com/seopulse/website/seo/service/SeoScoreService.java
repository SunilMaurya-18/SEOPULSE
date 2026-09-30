package com.seopulse.website.seo.service;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.entity.SeoIssue;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeoScoreService {

    private static final int MAX_SCORE = 100;

    private static final int ERROR_DEDUCTION = 10;

    private static final int WARNING_DEDUCTION = 4;

    private static final int INFO_DEDUCTION = 1;

    private final SeoIssueRepository seoIssueRepository;

    public int calculatePageScore(
            AuditPage page
    ) {

        List<SeoIssue> issues =
                seoIssueRepository.findByAuditPageId(
                        page.getId()
                );

        int score = MAX_SCORE;

        for (SeoIssue issue : issues) {

            score -= getDeduction(
                    issue.getSeverity()
            );
        }

        return Math.max(score, 0);
    }

    public int calculateAuditScore(
            List<AuditPage> pages
    ) {

        if (pages == null || pages.isEmpty()) {
            return 0;
        }

        int totalScore = 0;

        int analyzedPages = 0;

        for (AuditPage page : pages) {

            int pageScore =
                    calculatePageScore(page);

            totalScore += pageScore;

            analyzedPages++;
        }

        if (analyzedPages == 0) {
            return 0;
        }

        return Math.round(
                (float) totalScore / analyzedPages
        );
    }

    private int getDeduction(
            String severity
    ) {

        if (severity == null) {
            return 0;
        }

        return switch (
                severity.toUpperCase()
                ) {

            case "ERROR" -> ERROR_DEDUCTION;

            case "WARNING" -> WARNING_DEDUCTION;

            case "INFO" -> INFO_DEDUCTION;

            default -> 0;
        };
    }
}