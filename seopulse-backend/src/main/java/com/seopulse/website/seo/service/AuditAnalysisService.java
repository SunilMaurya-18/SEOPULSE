package com.seopulse.website.seo.service;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.service.AuditStateChangedException;
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

    /**
     * Analyzes crawled pages and moves the audit from ANALYZING to
     * COMPLETED. Failures propagate; the worker decides between retry
     * and FAILED.
     *
     * @throws AuditStateChangedException if the audit was cancelled meanwhile
     */
    public void analyzeAudit(Long auditId) {

        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.ANALYZING) {
            throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
        }

        log.info("Starting SEO analysis: auditId={}", auditId);

        List<AuditPage> crawledPages =
                auditPageRepository.findByAuditId(auditId)
                        .stream()
                        .filter(page -> page.getStatus() == AuditPageStatus.CRAWLED)
                        .toList();

        for (AuditPage page : crawledPages) {
            if (Thread.currentThread().isInterrupted()) {
                throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
            }
            seoAnalysisService.analyzeAndSave(page);
        }

        int score = seoScoreService.calculateAuditScore(crawledPages);

        int updated = auditRepository.completeAnalysis(
                auditId,
                AuditStatus.ANALYZING,
                AuditStatus.COMPLETED,
                score,
                crawledPages.size(),
                Instant.now()
        );

        if (updated == 0) {
            throw new AuditStateChangedException(auditId, AuditStatus.ANALYZING.name());
        }

        log.info(
                "SEO analysis completed: auditId={}, pagesAnalyzed={}, score={}",
                auditId,
                crawledPages.size(),
                score
        );
    }
}
