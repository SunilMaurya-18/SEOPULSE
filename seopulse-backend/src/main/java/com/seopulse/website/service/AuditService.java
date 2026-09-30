package com.seopulse.website.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.exception.DuplicateResourceException;
import com.seopulse.common.exception.EmailNotVerifiedException;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.dto.AuditPageResponse;
import com.seopulse.website.dto.AuditResponse;
import com.seopulse.website.dto.AuditSummaryResponse;
import com.seopulse.website.dto.SeoIssueResponse;
import com.seopulse.website.events.AuditEventPublisher;
import lombok.extern.slf4j.Slf4j;
import com.seopulse.website.repository.AuditOutboxRepository;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditOutbox;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.entity.SeoIssue;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditService {

    private static final List<AuditStatus> ACTIVE_STATUSES =
            List.of(AuditStatus.QUEUED, AuditStatus.CRAWLING, AuditStatus.ANALYZING);

    private final AuditRepository auditRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditPageRepository auditPageRepository;
    private final AuditOutboxRepository auditOutboxRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final ProjectAccessService projectAccessService;
    private final UrlValidator urlValidator;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;


    // ============================================================
    // CREATE AUDIT
    // ============================================================

    public AuditResponse createAudit(
            Long projectId,
            Long websiteId,
            Long userId
    ) {

        Website website =
                projectAccessService.requireOwnedWebsite(
                        projectId,
                        websiteId,
                        userId
                );


        if (authProperties.isRequireEmailVerification()) {
            boolean verified = userRepository.findById(userId)
                    .map(User::isEmailVerified)
                    .orElse(false);
            if (!verified) {
                throw new EmailNotVerifiedException();
            }
        }


        if (website.getStatus() != WebsiteStatus.ACTIVE) {

            throw new InvalidStateException(
                    "Website is not active"
            );
        }


        urlValidator.validate(
                website.getUrl()
        );


        boolean activeAuditExists =
                auditRepository
                        .existsByWebsiteIdAndStatusIn(
                                websiteId,
                                ACTIVE_STATUSES
                        );


        if (activeAuditExists) {

            throw new DuplicateResourceException(
                    "An audit is already running for this website"
            );
        }


        Audit audit =
                Audit.builder()
                        .website(website)
                        .status(AuditStatus.QUEUED)
                        .score(null)
                        .pagesCrawled(0)
                        .pagesAnalyzed(0)
                        .retryCount(0)
                        .maxRetries(3)
                        .startedAt(null)
                        .completedAt(null)
                        .errorMessage(null)
                        .build();


        Audit savedAudit =
                auditRepository.save(audit);


        AuditOutbox outbox =
                AuditOutbox.builder()
                        .audit(savedAudit)
                        .eventType("AUDIT_CREATED")
                        .published(false)
                        .build();


        auditOutboxRepository.save(outbox);

        log.info(
                "Audit created: auditId={}, websiteId={}, projectId={}",
                savedAudit.getId(),
                websiteId,
                projectId
        );

        return mapToResponse(savedAudit);
    }


    // ============================================================
    // CANCEL AUDIT
    // ============================================================

    /**
     * Cancels a queued or running audit. A running worker notices within
     * a few seconds and stops; its later status updates are conditional,
     * so they cannot overwrite CANCELLED.
     */
    public AuditResponse cancelAudit(
            Long projectId,
            Long auditId,
            Long userId
    ) {

        Audit audit = projectAccessService.requireOwnedAudit(projectId, auditId, userId);

        if (!audit.getStatus().isActive()) {
            throw new InvalidStateException("Only queued or running audits can be cancelled");
        }

        int updated = auditRepository.finish(
                auditId,
                ACTIVE_STATUSES,
                AuditStatus.CANCELLED,
                Instant.now(),
                "Cancelled by user"
        );

        if (updated == 0) {
            throw new InvalidStateException("Only queued or running audits can be cancelled");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditEventPublisher.publish(auditId, AuditStatus.CANCELLED);
            }
        });

        log.info("Audit cancelled: auditId={}, projectId={}", auditId, projectId);

        return auditRepository.findByIdWithWebsite(auditId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
    }


    /**
     * For server-side push after ownership was already checked.
     */
    @Transactional(readOnly = true)
    public Optional<AuditResponse> findAudit(Long auditId) {
        return auditRepository.findByIdWithWebsite(auditId).map(this::mapToResponse);
    }


    // ============================================================
    // GET AUDITS
    // ============================================================

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> getAudits(
            Long projectId,
            Long websiteId,
            Long userId,
            String status,
            Pageable pageable
    ) {

        projectAccessService.requireOwnedWebsite(
                projectId,
                websiteId,
                userId
        );


        Page<Audit> audits;


        AuditStatus normalizedStatus =
                normalizeStatus(status);


        if (normalizedStatus != null) {

            audits =
                    auditRepository
                            .findByWebsiteIdAndStatus(
                                    websiteId,
                                    normalizedStatus,
                                    pageable
                            );
        } else {

            audits =
                    auditRepository
                            .findByWebsiteId(
                                    websiteId,
                                    pageable
                            );
        }


        Page<AuditResponse> response =
                audits.map(this::mapToResponse);


        return PageResponse.from(response);
    }


    // ============================================================
    // GET SINGLE AUDIT
    // ============================================================

    @Transactional(readOnly = true)
    public AuditResponse getAudit(
            Long projectId,
            Long auditId,
            Long userId
    ) {

        Audit audit =
                projectAccessService.requireOwnedAudit(
                        projectId,
                        auditId,
                        userId
                );


        return mapToResponse(audit);
    }


    // ============================================================
    // GET AUDIT PAGES
    // ============================================================

    @Transactional(readOnly = true)
    public PageResponse<AuditPageResponse> getAuditPages(
            Long projectId,
            Long auditId,
            Long userId,
            Pageable pageable
    ) {

        projectAccessService.requireOwnedAudit(
                projectId,
                auditId,
                userId
        );


        Page<AuditPage> pages =
                auditPageRepository
                        .findByAuditId(
                                auditId,
                                pageable
                        );


        Page<AuditPageResponse> response =
                pages.map(
                        this::mapToAuditPageResponse
                );


        return PageResponse.from(response);
    }


    // ============================================================
    // GET SEO ISSUES
    // ============================================================

    @Transactional(readOnly = true)
    public PageResponse<SeoIssueResponse> getAuditIssues(
            Long projectId,
            Long auditId,
            Long userId,
            String severity,
            String ruleCode,
            Pageable pageable
    ) {

        projectAccessService.requireOwnedAudit(
                projectId,
                auditId,
                userId
        );


        String normalizedSeverity =
                normalizeSeverity(severity);


        String normalizedRuleCode =
                normalizeRuleCode(ruleCode);


        Page<SeoIssue> issues;


        // Both filters
        if (normalizedSeverity != null
                && normalizedRuleCode != null) {

            issues =
                    seoIssueRepository
                            .findByAuditPageAuditIdAndSeverityIgnoreCaseAndRuleCode(
                                    auditId,
                                    normalizedSeverity,
                                    normalizedRuleCode,
                                    pageable
                            );
        }

        // Severity only
        else if (normalizedSeverity != null) {

            issues =
                    seoIssueRepository
                            .findByAuditPageAuditIdAndSeverityIgnoreCase(
                                    auditId,
                                    normalizedSeverity,
                                    pageable
                            );
        }

        // Rule code only
        else if (normalizedRuleCode != null) {

            issues =
                    seoIssueRepository
                            .findByAuditPageAuditIdAndRuleCode(
                                    auditId,
                                    normalizedRuleCode,
                                    pageable
                            );
        }

        // No filters
        else {

            issues =
                    seoIssueRepository
                            .findByAuditPageAuditId(
                                    auditId,
                                    pageable
                            );
        }


        Page<SeoIssueResponse> response =
                issues.map(
                        this::mapToSeoIssueResponse
                );


        return PageResponse.from(response);
    }


    // ============================================================
    // GET AUDIT SUMMARY
    // ============================================================

    @Transactional(readOnly = true)
    public AuditSummaryResponse getAuditSummary(
            Long projectId,
            Long auditId,
            Long userId
    ) {

        Audit audit =
                projectAccessService.requireOwnedAudit(
                        projectId,
                        auditId,
                        userId
                );


        long totalIssues =
                seoIssueRepository
                        .countByAuditPageAuditId(auditId);


        long errorCount =
                seoIssueRepository
                        .countByAuditPageAuditIdAndSeverityIgnoreCase(
                                auditId,
                                "ERROR"
                        );


        long warningCount =
                seoIssueRepository
                        .countByAuditPageAuditIdAndSeverityIgnoreCase(
                                auditId,
                                "WARNING"
                        );


        long infoCount =
                seoIssueRepository
                        .countByAuditPageAuditIdAndSeverityIgnoreCase(
                                auditId,
                                "INFO"
                        );


        log.debug(
                "Audit summary retrieved: auditId={}, projectId={}, score={}, issues={}",
                auditId,
                projectId,
                audit.getScore(),
                totalIssues
        );

        return new AuditSummaryResponse(

                audit.getId(),

                audit.getWebsite()
                        .getId(),

                audit.getWebsite()
                        .getUrl(),

                audit.getStatus(),

                audit.getScore(),

                audit.getPagesCrawled(),

                audit.getPagesAnalyzed(),

                totalIssues,

                errorCount,

                warningCount,

                infoCount,

                audit.getStartedAt(),

                audit.getCompletedAt()
        );
    }


    // ============================================================
    // GET LATEST AUDIT
    // ============================================================

    @Transactional(readOnly = true)
    public AuditResponse getLatestAudit(
            Long projectId,
            Long websiteId,
            Long userId
    ) {

        projectAccessService.requireOwnedWebsite(
                projectId,
                websiteId,
                userId
        );


        return auditRepository
                .findFirstByWebsiteIdOrderByCreatedAtDesc(websiteId)
                .map(this::mapToResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No audits found for this website"
                        )
                );
    }


    // ============================================================
    // NORMALIZE STATUS
    // ============================================================

    private AuditStatus normalizeStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return null;
        }


        String value =
                status
                        .trim()
                        .toUpperCase();


        try {

            return AuditStatus.valueOf(value);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid status. Allowed values: QUEUED, CRAWLING, ANALYZING, COMPLETED, FAILED, CANCELLED"
            );
        }
    }


    // ============================================================
    // NORMALIZE SEVERITY
    // ============================================================

    private String normalizeSeverity(
            String severity
    ) {

        if (severity == null
                || severity.isBlank()) {

            return null;
        }


        String value =
                severity
                        .trim()
                        .toUpperCase();


        if (!value.equals("ERROR")
                && !value.equals("WARNING")
                && !value.equals("INFO")) {

            throw new IllegalArgumentException(
                    "Invalid severity. Allowed values: ERROR, WARNING, INFO"
            );
        }


        return value;
    }


    // ============================================================
    // NORMALIZE RULE CODE
    // ============================================================

    private String normalizeRuleCode(
            String ruleCode
    ) {

        if (ruleCode == null
                || ruleCode.isBlank()) {

            return null;
        }


        String value =
                ruleCode.trim();


        if (value.length() > 100) {

            throw new IllegalArgumentException(
                    "Rule code must not exceed 100 characters"
            );
        }


        return value;
    }


    // ============================================================
    // AUDIT → DTO
    // ============================================================

    private AuditResponse mapToResponse(
            Audit audit
    ) {

        return new AuditResponse(

                audit.getId(),

                audit.getWebsite()
                        .getId(),

                audit.getWebsite()
                        .getUrl(),

                audit.getStatus(),

                audit.getScore(),

                audit.getPagesCrawled(),

                audit.getPagesAnalyzed(),

                audit.getStartedAt(),

                audit.getCompletedAt(),

                audit.getErrorMessage(),

                audit.getCreatedAt()
        );
    }


    // ============================================================
    // AUDIT PAGE → DTO
    // ============================================================

    private AuditPageResponse mapToAuditPageResponse(
            AuditPage page
    ) {

        return new AuditPageResponse(

                page.getId(),

                page.getAudit()
                        .getId(),

                page.getUrl(),

                page.getStatus(),

                page.getStatusCode(),

                page.getContentType(),

                page.getTitle(),

                page.getMetaDescription(),

                page.getCanonicalUrl(),

                page.getWordCount(),

                page.getH1Count(),

                page.getImageCount(),

                page.getImagesWithoutAlt(),

                page.getInternalLinkCount(),

                page.getExternalLinkCount(),

                page.getDepth(),

                page.getFinalUrl(),

                page.getRedirectChain(),

                page.getSkipReason(),

                page.getCrawledAt(),

                page.getCreatedAt()
        );
    }


    // ============================================================
    // SEO ISSUE → DTO
    // ============================================================

    private SeoIssueResponse mapToSeoIssueResponse(
            SeoIssue issue
    ) {

        return new SeoIssueResponse(

                issue.getId(),

                issue.getAuditPage()
                        .getAudit()
                        .getId(),

                issue.getAuditPage()
                        .getId(),

                issue.getAuditPage()
                        .getUrl(),

                issue.getRuleCode(),

                issue.getSeverity(),

                issue.getMessage(),

                issue.getRecommendations(),

                issue.getCreatedAt()
        );
    }
}