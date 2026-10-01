package com.seopulse.retention;

import com.seopulse.alert.AlertOutboxRepository;
import com.seopulse.billing.EntitlementService;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.report.Report;
import com.seopulse.report.ReportRepository;
import com.seopulse.report.ReportShareRepository;
import com.seopulse.report.storage.ReportStorage;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Removes page- and issue-level audit data once it is older than the
 * organization's plan allows. Audit rows keep their score, counts and
 * category scores, so trends and history survive.
 */
@Service
@Slf4j
public class RetentionService {

    static final int BATCH_SIZE = 50;
    static final List<AuditStatus> FINISHED = List.of(AuditStatus.COMPLETED, AuditStatus.FAILED, AuditStatus.CANCELLED);
    static final Duration INACTIVE_SHARE_KEEP = Duration.ofDays(30);
    static final Duration DELIVERED_ALERT_KEEP = Duration.ofDays(90);

    private final OrganizationRepository organizationRepository;
    private final EntitlementService entitlementService;
    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final ReportRepository reportRepository;
    private final ReportShareRepository shareRepository;
    private final AlertOutboxRepository alertOutboxRepository;
    private final ReportStorage reportStorage;
    private final TransactionTemplate tx;

    public RetentionService(
            OrganizationRepository organizationRepository,
            EntitlementService entitlementService,
            AuditRepository auditRepository,
            AuditPageRepository auditPageRepository,
            ReportRepository reportRepository,
            ReportShareRepository shareRepository,
            AlertOutboxRepository alertOutboxRepository,
            ReportStorage reportStorage,
            PlatformTransactionManager transactionManager
    ) {
        this.organizationRepository = organizationRepository;
        this.entitlementService = entitlementService;
        this.auditRepository = auditRepository;
        this.auditPageRepository = auditPageRepository;
        this.reportRepository = reportRepository;
        this.shareRepository = shareRepository;
        this.alertOutboxRepository = alertOutboxRepository;
        this.reportStorage = reportStorage;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public record Result(int auditsPurged, int reportsDeleted, int sharesDeleted, int alertsDeleted) {
    }

    public Result purge(Instant now) {
        int audits = 0;
        int reports = 0;

        for (Long organizationId : organizationRepository.findAllIds()) {
            try {
                int[] counts = purgeOrganization(organizationId, now);
                audits += counts[0];
                reports += counts[1];
            } catch (RuntimeException ex) {
                log.error("Retention failed for organization {}", organizationId, ex);
            }
        }

        int shares = shareRepository.deleteInactiveBefore(now.minus(INACTIVE_SHARE_KEEP));
        Integer alerts = tx.execute(status -> alertOutboxRepository.deleteDeliveredBefore(now.minus(DELIVERED_ALERT_KEEP)));

        Result result = new Result(audits, reports, shares, alerts == null ? 0 : alerts);
        log.info("Retention finished: {}", result);
        return result;
    }

    private int[] purgeOrganization(Long organizationId, Instant now) {
        int days = entitlementService.limitsFor(organizationId).retentionDays();
        if (days <= 0) {
            return new int[]{0, 0};
        }
        Instant before = now.minus(Duration.ofDays(days));
        int audits = 0;
        int reports = 0;

        while (true) {
            List<Long> ids = auditRepository.findPurgeable(organizationId, FINISHED, before, PageRequest.of(0, BATCH_SIZE));
            if (ids.isEmpty()) {
                break;
            }

            List<Report> stored = reportRepository.findStoredByAuditIds(ids);
            for (Report report : stored) {
                try {
                    reportStorage.delete(report.getStorageKey());
                } catch (IOException ex) {
                    log.warn("Could not delete report file: reportId={}, error={}", report.getId(), ex.getMessage());
                }
            }

            tx.executeWithoutResult(status -> {
                auditPageRepository.deleteByAuditIds(ids);
                reportRepository.deleteAllByIdInBatch(stored.stream().map(Report::getId).toList());
                auditRepository.markPurged(ids, now);
            });

            audits += ids.size();
            reports += stored.size();
        }

        if (audits > 0) {
            log.info("Retention purged {} audits and {} reports for organization {} (older than {} days)",
                    audits, reports, organizationId, days);
        }
        return new int[]{audits, reports};
    }
}
