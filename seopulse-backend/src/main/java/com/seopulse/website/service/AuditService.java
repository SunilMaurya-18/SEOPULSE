package com.seopulse.website.service;

import com.seopulse.billing.EntitlementService;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.service.OrganizationAccessService;
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
import com.seopulse.website.entity.AuditTrigger;
import com.seopulse.website.entity.Website;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleDefinition;
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
    private final EntitlementService entitlementService;
    private final OrganizationAccessService organizationAccessService;
    private final EmailOutboxService emailOutboxService;
    private final RuleCatalog ruleCatalog;


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


        if (website.getStatus() == WebsiteStatus.LOCKED) {
            throw new InvalidStateException("This website is locked on the current plan");
        }

        if (website.getStatus() != WebsiteStatus.ACTIVE) {

            throw new InvalidStateException(
                    "Website is not active"
            );
        }

        Long organizationId = website.getProject().getOrganization().getId();
        organizationAccessService.requireRole(organizationId, userId, OrganizationRole.MEMBER);
        entitlementService.consumeAudit(organizationId);


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


        Audit savedAudit = queueAudit(website, AuditTrigger.MANUAL);

        log.info(
                "Audit created: auditId={}, websiteId={}, projectId={}",
                savedAudit.getId(),
                websiteId,
                projectId
        );

        return mapToResponse(savedAudit);
    }


    public enum ScheduledAuditOutcome {
        CREATED,
        WEBSITE_INACTIVE,
        ALREADY_RUNNING,
        QUOTA_EXHAUSTED
    }

    public record ScheduledAuditResult(ScheduledAuditOutcome outcome, Long auditId) {
    }

    /**
     * Queues an audit for a schedule. Expected refusals are returned rather
     * than thrown, so the dispatcher's transaction (which also advances the
     * schedule) is never marked rollback-only.
     */
    public ScheduledAuditResult createScheduledAudit(Website website) {

        if (website.getStatus() != WebsiteStatus.ACTIVE) {
            return new ScheduledAuditResult(ScheduledAuditOutcome.WEBSITE_INACTIVE, null);
        }

        if (auditRepository.existsByWebsiteIdAndStatusIn(website.getId(), ACTIVE_STATUSES)) {
            return new ScheduledAuditResult(ScheduledAuditOutcome.ALREADY_RUNNING, null);
        }

        Long organizationId = website.getProject().getOrganization().getId();
        if (!entitlementService.tryConsumeAudit(organizationId)) {
            return new ScheduledAuditResult(ScheduledAuditOutcome.QUOTA_EXHAUSTED, null);
        }

        Audit audit = queueAudit(website, AuditTrigger.SCHEDULED);
        log.info("Scheduled audit created: auditId={}, websiteId={}", audit.getId(), website.getId());
        return new ScheduledAuditResult(ScheduledAuditOutcome.CREATED, audit.getId());
    }

    private Audit queueAudit(Website website, AuditTrigger trigger) {

        Audit savedAudit = auditRepository.save(
                Audit.builder()
                        .website(website)
                        .status(AuditStatus.QUEUED)
                        .pagesCrawled(0)
                        .pagesAnalyzed(0)
                        .retryCount(0)
                        .maxRetries(3)
                        .triggeredBy(trigger)
                        .build()
        );

        auditOutboxRepository.save(
                AuditOutbox.builder()
                        .audit(savedAudit)
                        .eventType("AUDIT_CREATED")
                        .published(false)
                        .build()
        );

        return savedAudit;
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


        boolean aggregated = audit.getIssueCount() != null;

        long totalIssues = aggregated
                ? audit.getIssueCount()
                : seoIssueRepository.countByAuditPageAuditId(auditId);

        long errorCount = aggregated && audit.getErrorCount() != null
                ? audit.getErrorCount()
                : seoIssueRepository.countByAuditPageAuditIdAndSeverityIgnoreCase(auditId, "ERROR");

        long warningCount = aggregated && audit.getWarningCount() != null
                ? audit.getWarningCount()
                : seoIssueRepository.countByAuditPageAuditIdAndSeverityIgnoreCase(auditId, "WARNING");

        long infoCount = aggregated && audit.getInfoCount() != null
                ? audit.getInfoCount()
                : seoIssueRepository.countByAuditPageAuditIdAndSeverityIgnoreCase(auditId, "INFO");


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

                audit.getCompletedAt(),

                audit.getCategoryScores(),

                audit.getScoreVersion(),

                audit.getDetailsPurgedAt() != null
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

                audit.getCreatedAt(),

                audit.getTriggeredBy(),

                audit.getIssueCount(),

                audit.getErrorCount(),

                audit.getScoreVersion()
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


    public int emailReport(Long projectId, Long auditId, Long userId, List<String> recipients, String note) {
        Audit audit = projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        if (audit.getStatus() != AuditStatus.COMPLETED) {
            throw new InvalidStateException("The report is not ready to email yet");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (authProperties.isRequireEmailVerification() && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        List<String> targets = (recipients == null ? List.<String>of() : recipients).stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        if (targets.isEmpty()) {
            targets = List.of(user.getEmail());
        }
        if (targets.size() > 5) {
            throw new InvalidStateException("Send the report to at most 5 recipients at a time");
        }

        String url = authProperties.getAppBaseUrl() + "/audits/" + audit.getId();
        String summary = "%s shared the SEO audit for %s. Health score %s, %d pages crawled.".formatted(
                user.getName(),
                audit.getWebsite().getUrl(),
                audit.getScore() == null ? "n/a" : audit.getScore(),
                audit.getPagesCrawled()
        );
        String trimmedNote = note == null ? "" : note.trim();
        if (trimmedNote.length() > 1000) {
            trimmedNote = trimmedNote.substring(0, 1000);
        }
        String intro = trimmedNote.isEmpty() ? summary : summary + "\n\n" + trimmedNote;

        for (String to : targets) {
            emailOutboxService.enqueue(
                    to,
                    "SEO audit report: " + audit.getWebsite().getName(),
                    EmailTemplates.text(intro, url),
                    EmailTemplates.html("SEO audit report", intro, "Open report", url)
            );
        }
        return targets.size();
    }

    // ============================================================
    // SEO ISSUE → DTO
    // ============================================================

    private SeoIssueResponse mapToSeoIssueResponse(
            SeoIssue issue
    ) {

        RuleDefinition rule = ruleCatalog.get(issue.getRuleCode(), issue.getSeverity());

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

                issue.getCreatedAt(),

                issue.getCategory() != null ? issue.getCategory() : rule.category().name(),

                issue.getFingerprint(),

                rule.title(),

                rule.helpUrl()
        );
    }
}