package com.seopulse.website.seo.service;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.entity.SeoIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAnalysisService {

    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final SeoAnalysisService seoAnalysisService;
    private final SeoScoreService seoScoreService;

    public void analyzeAudit(Long auditId) {

        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Audit not found: " + auditId
                        )
                );

        if (audit.getStatus() != AuditStatus.ANALYZING) {

            log.warn(
                    "Skipping analysis for audit {} because status is {}",
                    auditId,
                    audit.getStatus()
            );

            return;
        }

        try {

            log.info(
                    "Starting SEO analysis: auditId={}",
                    auditId
            );

            List<AuditPage> crawledPages =
                    auditPageRepository.findByAuditId(auditId)
                            .stream()
                            .filter(page -> page.getStatus() == AuditPageStatus.CRAWLED)
                            .toList();

            crawledPages.forEach(this::analyzePage);

            int analyzedCount = crawledPages.size();

            int score =
                    seoScoreService.calculateAuditScore(crawledPages);

            audit.setPagesAnalyzed(analyzedCount);

            audit.setScore(score);

            audit.setStatus(
                    AuditStatus.COMPLETED
            );

            audit.setCompletedAt(
                    Instant.now()
            );

            auditRepository.save(audit);

            log.info(
                    "SEO analysis completed: auditId={}, pagesAnalyzed={}",
                    auditId,
                    analyzedCount
            );

        } catch (Exception ex) {

            log.error(
                    "SEO analysis failed: auditId={}",
                    auditId,
                    ex
            );

            audit.setStatus(AuditStatus.FAILED);
            audit.setCompletedAt(Instant.now());
            audit.setErrorMessage(
                    buildErrorMessage(ex)
            );

            auditRepository.save(audit);

            throw ex;
        }
    }

    private void analyzePage(AuditPage page) {

        log.debug(
                "Analyzing page: {}",
                page.getUrl()
        );

        List<SeoIssue> issues =
                seoAnalysisService.analyzeAndSave(page);

        log.debug(
                "Page analysis completed: url={}, issues={}",
                page.getUrl(),
                issues.size()
        );
    }

    private String buildErrorMessage(Exception ex) {

        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            return "SEO analysis failed";
        }

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}