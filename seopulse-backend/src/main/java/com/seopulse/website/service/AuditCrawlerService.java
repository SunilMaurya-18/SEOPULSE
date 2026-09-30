package com.seopulse.website.service;

import com.seopulse.common.metrics.AuditMetrics;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuditCrawlerService {

    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final WebsiteCrawler websiteCrawler;
    private final TransactionTemplate transactionTemplate;
    private final AuditMetrics metrics;

    public AuditCrawlerService(
            AuditRepository auditRepository,
            AuditPageRepository auditPageRepository,
            WebsiteCrawler websiteCrawler,
            TransactionTemplate transactionTemplate,
            AuditMetrics metrics
    ) {
        this.auditRepository = auditRepository;
        this.auditPageRepository = auditPageRepository;
        this.websiteCrawler = websiteCrawler;
        this.transactionTemplate = transactionTemplate;
        this.metrics = metrics;
    }

    /**
     * Crawls the audit's website and stores the pages, moving the audit
     * from CRAWLING to ANALYZING. The crawl itself runs outside any
     * transaction so no DB connection is held while waiting on the network.
     * Failures propagate; the worker decides between retry and FAILED.
     *
     * @throws AuditStateChangedException if the audit was cancelled meanwhile
     */
    public void crawlAudit(Long auditId) throws InterruptedException {

        Audit audit = auditRepository.findByIdWithWebsite(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.CRAWLING) {
            throw new AuditStateChangedException(auditId, AuditStatus.CRAWLING.name());
        }

        String websiteUrl = audit.getWebsite().getUrl();

        log.info("Starting website crawl: auditId={}, url={}", auditId, websiteUrl);

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

            int crawled = (int) result.pages().stream()
                    .filter(page -> page.outcome() == CrawledPage.Outcome.CRAWLED)
                    .count();

            int updated = auditRepository.completeCrawl(
                    auditId,
                    AuditStatus.CRAWLING,
                    AuditStatus.ANALYZING,
                    crawled,
                    result.timedOut() ? "Crawl time budget reached; results are partial" : null
            );

            if (updated == 0) {
                // Rolls back the saved pages as well.
                throw new AuditStateChangedException(auditId, AuditStatus.CRAWLING.name());
            }
        });

        result.pages().stream()
                .collect(Collectors.groupingBy(CrawledPage::outcome, Collectors.counting()))
                .forEach((outcome, count) -> metrics.pagesCrawled(outcome.name(), count));

        log.info("Audit {} moved to ANALYZING", auditId);
    }

    private void savePages(
            Audit audit,
            List<CrawledPage> pages
    ) {

        Instant crawledAt = Instant.now();

        Map<String, CrawledPage> unique = pages.stream()
                .filter(page -> page.url().length() <= 2048)
                .collect(Collectors.toMap(CrawledPage::url, Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));

        for (CrawledPage page : unique.values()) {

            if (auditPageRepository.existsByAuditIdAndUrl(audit.getId(), page.url())) {
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
}
