package com.seopulse.report;

import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.AuditTrigger;
import com.seopulse.website.repository.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Worker-side report processing: renders queued PDFs and emails the
 * results of scheduled audits.
 */
@Component
@Slf4j
public class ReportGenerator {

    static final int MAX_PER_RUN = 5;
    static final Duration STALE_AFTER = Duration.ofMinutes(15);

    private final ReportRepository reportRepository;
    private final AuditRepository auditRepository;
    private final ReportService reportService;
    private final ReportProperties properties;
    private final OrganizationMemberRepository memberRepository;
    private final EmailOutboxService emailOutboxService;
    private final TransactionTemplate tx;

    public ReportGenerator(
            ReportRepository reportRepository,
            AuditRepository auditRepository,
            ReportService reportService,
            ReportProperties properties,
            OrganizationMemberRepository memberRepository,
            EmailOutboxService emailOutboxService,
            PlatformTransactionManager transactionManager
    ) {
        this.reportRepository = reportRepository;
        this.auditRepository = auditRepository;
        this.reportService = reportService;
        this.properties = properties;
        this.memberRepository = memberRepository;
        this.emailOutboxService = emailOutboxService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Queues a PDF for a completed scheduled audit, to be emailed to the org's owners and admins. */
    public void onScheduledAuditCompleted(Long auditId) {
        tx.executeWithoutResult(status -> {
            Audit audit = auditRepository.findByIdWithWebsite(auditId).orElse(null);
            if (audit == null || audit.getStatus() != AuditStatus.COMPLETED || audit.getTriggeredBy() != AuditTrigger.SCHEDULED) {
                return;
            }
            Long organizationId = audit.getWebsite().getProject().getOrganization().getId();
            String recipients = String.join(",", adminEmails(organizationId));
            if (recipients.isEmpty()) {
                return;
            }
            if (recipients.length() > 1000) {
                recipients = recipients.substring(0, recipients.lastIndexOf(',', 1000));
            }
            reportRepository.save(reportService.newReport(audit, organizationId, null, recipients));
        });
    }

    public int processPending() {
        reportRepository.requeueStale(Instant.now().minus(STALE_AFTER));
        int processed = 0;
        while (processed < MAX_PER_RUN) {
            Long reportId = tx.execute(status -> reportRepository.lockNextPending()
                    .map(report -> {
                        report.setStatus(ReportStatus.GENERATING);
                        report.setAttempts(report.getAttempts() + 1);
                        return report.getId();
                    })
                    .orElse(null));
            if (reportId == null) {
                break;
            }
            generate(reportId);
            processed++;
        }
        return processed;
    }

    private void generate(Long reportId) {
        try {
            tx.executeWithoutResult(status -> {
                Report report = reportRepository.findById(reportId).orElseThrow();
                Audit audit = auditRepository.findByIdWithWebsite(report.getAuditId())
                        .orElseThrow(() -> new IllegalStateException("Audit no longer exists"));
                byte[] pdf = reportService.render(audit, report.isWhiteLabel(), report.isWatermark());
                reportService.store(report, audit, pdf);
                report.setStatus(ReportStatus.READY);
                report.setErrorMessage(null);
                if (report.getEmailTo() != null && !report.getEmailTo().isBlank()) {
                    email(report, audit);
                }
            });
            log.info("Report generated: reportId={}", reportId);
        } catch (RuntimeException ex) {
            log.error("Report generation failed: reportId={}", reportId, ex);
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            tx.executeWithoutResult(status -> reportRepository.findById(reportId).ifPresent(report -> {
                report.setStatus(report.getAttempts() >= properties.getMaxAttempts() ? ReportStatus.FAILED : ReportStatus.PENDING);
                report.setErrorMessage(message.length() > 500 ? message.substring(0, 500) : message);
            }));
        }
    }

    private void email(Report report, Audit audit) {
        ReportService.CreatedShare share = reportService.createShareInternal(
                audit.getId(), null, Duration.ofDays(properties.getDefaultShareDays()));
        String site = audit.getWebsite().getName();
        String subject = "Scheduled audit: " + site + " scored " + (audit.getScore() == null ? "-" : audit.getScore());
        String intro = "The scheduled audit of " + audit.getWebsite().getUrl() + " has finished with a score of "
                + (audit.getScore() == null ? "-" : audit.getScore()) + ". Open the report to see the details "
                + "or download the PDF. The link works for " + properties.getDefaultShareDays() + " days.";
        String text = EmailTemplates.text(intro, share.url());
        String html = EmailTemplates.html(subject, intro, "Open report", share.url());
        Arrays.stream(report.getEmailTo().split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .distinct()
                .forEach(address -> emailOutboxService.enqueue(address, subject, text, html));
    }

    private List<String> adminEmails(Long organizationId) {
        return memberRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .filter(member -> member.getRole().atLeast(OrganizationRole.ADMIN))
                .map(OrganizationMember::getUser)
                .map(user -> user.getEmail())
                .distinct()
                .toList();
    }
}
