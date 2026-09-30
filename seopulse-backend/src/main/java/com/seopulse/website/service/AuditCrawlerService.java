package com.seopulse.website.service;

import com.seopulse.website.crawler.CrawlResult;
import com.seopulse.website.crawler.CrawledPage;
import com.seopulse.website.crawler.WebsiteCrawler;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class AuditCrawlerService {

    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final WebsiteCrawler websiteCrawler;
    private final TransactionTemplate transactionTemplate;

    public AuditCrawlerService(
            AuditRepository auditRepository,
            AuditPageRepository auditPageRepository,
            WebsiteCrawler websiteCrawler,
            TransactionTemplate transactionTemplate
    ) {
        this.auditRepository = auditRepository;
        this.auditPageRepository = auditPageRepository;
        this.websiteCrawler = websiteCrawler;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Crawls the audit's website and stores the pages. The crawl itself
     * runs outside any transaction so no DB connection is held while
     * waiting on the network.
     */
    public void crawlAudit(Long auditId) throws InterruptedException {

        Audit audit = auditRepository.findByIdWithWebsite(auditId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Audit not found: " + auditId
                        )
                );

        if (audit.getStatus() != AuditStatus.CRAWLING) {
            log.warn(
                    "Skipping crawl for audit {} because status is {}",
                    auditId,
                    audit.getStatus()
            );
            return;
        }

        String websiteUrl = audit.getWebsite().getUrl();

        try {

            log.info(
                    "Starting website crawl: auditId={}, url={}",
                    auditId,
                    websiteUrl
            );

            CrawlResult result = websiteCrawler.crawl(websiteUrl);

            log.info(
                    "Crawl completed: auditId={}, pages={}, timedOut={}",
                    auditId,
                    result.pages().size(),
                    result.timedOut()
            );

            transactionTemplate.executeWithoutResult(status -> {

                Audit managed = auditRepository.findById(auditId)
                        .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

                savePages(managed, result.pages());

                managed.setPagesCrawled(
                        (int) result.pages().stream()
                                .filter(page -> page.outcome() == CrawledPage.Outcome.CRAWLED)
                                .count()
                );
                managed.setStatus(AuditStatus.ANALYZING);

                if (result.timedOut()) {
                    managed.setErrorMessage("Crawl time budget reached; results are partial");
                }

                auditRepository.save(managed);
            });

            log.info(
                    "Audit {} moved to ANALYZING",
                    auditId
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markFailed(auditId, "Website crawl was interrupted");
            throw ex;
        } catch (RuntimeException ex) {

            log.error(
                    "Website crawl failed: auditId={}",
                    auditId,
                    ex
            );

            markFailed(auditId, buildErrorMessage(ex));

            throw ex;
        }
    }

    private void markFailed(Long auditId, String message) {
        transactionTemplate.executeWithoutResult(status ->
                auditRepository.findById(auditId).ifPresent(audit -> {
                    audit.setStatus(AuditStatus.FAILED);
                    audit.setCompletedAt(Instant.now());
                    audit.setErrorMessage(message);
                    auditRepository.save(audit);
                })
        );
    }

    private void savePages(
            Audit audit,
            List<CrawledPage> pages
    ) {

        Instant crawledAt = Instant.now();

        for (CrawledPage page : pages) {

            if (page.url().length() > 2048
                    || auditPageRepository.existsByAuditIdAndUrl(audit.getId(), page.url())) {
                continue;
            }

            AuditPage auditPage =
                    AuditPage.builder()
                            .audit(audit)
                            .url(page.url())
                            .status(toStatus(page.outcome()))
                            .statusCode(page.status() > 0 ? page.status() : null)
                            .contentType(truncate(page.contentType(), 100))
                            .title(truncate(page.title(), 500))
                            .metaDescription(truncate(page.metaDescription(), 1000))
                            .canonicalUrl(truncate(page.canonicalUrl(), 2048))
                            .wordCount(page.wordCount())
                            .h1Count(page.h1Count())
                            .imageCount(page.imageCount())
                            .imagesWithoutAlt(page.imagesWithoutAlt())
                            .internalLinkCount(page.internalLinkCount())
                            .externalLinkCount(page.externalLinkCount())
                            .depth(page.depth())
                            .finalUrl(truncate(page.finalUrl(), 2048))
                            .redirectChain(page.redirectChain())
                            .skipReason(truncate(page.skipReason(), 500))
                            .crawledAt(crawledAt)
                            .build();

            auditPageRepository.save(auditPage);
        }
    }

    private static AuditPageStatus toStatus(CrawledPage.Outcome outcome) {
        return switch (outcome) {
            case CRAWLED -> AuditPageStatus.CRAWLED;
            case REDIRECT -> AuditPageStatus.REDIRECT;
            case SKIPPED_ROBOTS -> AuditPageStatus.SKIPPED_ROBOTS;
            case TOO_LARGE -> AuditPageStatus.TOO_LARGE;
            case FAILED -> AuditPageStatus.FAILED;
        };
    }

    private static String truncate(String value, int maxLength) {
        return value != null && value.length() > maxLength
                ? value.substring(0, maxLength)
                : value;
    }

    private String buildErrorMessage(Exception ex) {

        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            return "Website crawl failed";
        }

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}
