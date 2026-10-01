package com.seopulse.report;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.service.SecureTokens;
import com.seopulse.billing.EntitlementService;
import com.seopulse.billing.PlanLimits;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.service.OrganizationAccessService;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.report.render.PdfRenderer;
import com.seopulse.report.render.ReportHtmlRenderer;
import com.seopulse.report.render.ReportModel;
import com.seopulse.report.render.ReportModelFactory;
import com.seopulse.report.storage.ReportStorage;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final List<ReportStatus> IN_PROGRESS = List.of(ReportStatus.PENDING, ReportStatus.GENERATING);

    private final ReportRepository reportRepository;
    private final ReportShareRepository shareRepository;
    private final AuditRepository auditRepository;
    private final ProjectAccessService projectAccessService;
    private final OrganizationAccessService organizationAccessService;
    private final EntitlementService entitlementService;
    private final ReportModelFactory modelFactory;
    private final ReportHtmlRenderer htmlRenderer;
    private final PdfRenderer pdfRenderer;
    private final ReportStorage storage;
    private final ReportUrlSigner urlSigner;
    private final ReportProperties properties;
    private final AuthProperties authProperties;

    // ---- authenticated ------------------------------------------------------

    /** Queues a PDF for the audit; returns the in-progress report if one is already queued. */
    @Transactional
    public ReportResponse request(Long projectId, Long auditId, Long userId) {
        Audit audit = requireCompletedAudit(projectId, auditId, userId);
        Long organizationId = organizationId(audit);
        organizationAccessService.requireRole(organizationId, userId, OrganizationRole.MEMBER);

        List<Report> active = reportRepository.findByAuditIdAndStatusIn(auditId, IN_PROGRESS);
        if (!active.isEmpty()) {
            return ReportResponse.from(active.getFirst());
        }
        return ReportResponse.from(reportRepository.save(newReport(audit, organizationId, userId, null)));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> list(Long projectId, Long auditId, Long userId) {
        projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        return reportRepository.findTop10ByAuditIdOrderByCreatedAtDesc(auditId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportUrlSigner.SignedUrl downloadUrl(Long projectId, Long auditId, Long reportId, Long userId) {
        projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        Report report = reportRepository.findByIdAndAuditId(reportId, auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (report.getStatus() != ReportStatus.READY) {
            throw new InvalidStateException("The report is not ready yet");
        }
        return urlSigner.sign(report.getId(), Instant.now());
    }

    @Transactional
    public ShareResponse createShare(Long projectId, Long auditId, Long userId, Integer expiresInDays) {
        Audit audit = requireCompletedAudit(projectId, auditId, userId);
        organizationAccessService.requireRole(organizationId(audit), userId, OrganizationRole.MEMBER);

        int days = expiresInDays == null ? properties.getDefaultShareDays() : expiresInDays;
        if (days < 1 || days > properties.getMaxShareDays()) {
            throw new IllegalArgumentException("Share links can last between 1 and " + properties.getMaxShareDays() + " days");
        }
        CreatedShare created = createShareInternal(auditId, userId, Duration.ofDays(days));
        return ShareResponse.from(created.share(), created.url());
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> listShares(Long projectId, Long auditId, Long userId) {
        projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        return shareRepository.findByAuditIdOrderByCreatedAtDesc(auditId).stream()
                .map(share -> ShareResponse.from(share, null))
                .toList();
    }

    @Transactional
    public void revokeShare(Long projectId, Long auditId, Long shareId, Long userId) {
        Audit audit = projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        organizationAccessService.requireRole(organizationId(audit), userId, OrganizationRole.MEMBER);
        ReportShare share = shareRepository.findByIdAndAuditId(shareId, auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        if (share.getRevokedAt() == null) {
            share.setRevokedAt(Instant.now());
        }
    }

    // ---- public -------------------------------------------------------------

    @Transactional
    public ReportModel sharedReport(String token) {
        ReportShare share = requireActiveShare(token);
        shareRepository.recordView(share.getId(), Instant.now());
        Audit audit = requireAudit(share.getAuditId());
        Flags flags = flags(organizationId(audit));
        return modelFactory.build(audit, flags.whiteLabel(), flags.watermark());
    }

    /** The newest stored PDF for the shared audit, rendering and storing one if needed. */
    @Transactional
    public byte[] sharedPdf(String token) {
        ReportShare share = requireActiveShare(token);
        Audit audit = requireAudit(share.getAuditId());
        Long organizationId = organizationId(audit);
        Flags flags = flags(organizationId);

        var ready = reportRepository.findFirstByAuditIdAndStatusAndWatermarkOrderByCreatedAtDesc(
                audit.getId(), ReportStatus.READY, flags.watermark());
        if (ready.isPresent() && ready.get().isWhiteLabel() == flags.whiteLabel()) {
            try {
                return storage.get(ready.get().getStorageKey());
            } catch (IOException ex) {
                log.warn("Stored report missing, re-rendering: reportId={}", ready.get().getId());
            }
        }

        byte[] pdf = render(audit, flags.whiteLabel(), flags.watermark());
        Report report = newReport(audit, organizationId, null, null);
        report.setStatus(ReportStatus.READY);
        report.setAttempts(1);
        report = reportRepository.save(report);
        store(report, audit, pdf);
        return pdf;
    }

    @Transactional(readOnly = true)
    public byte[] signedFile(Long reportId, long expires, String signature) {
        if (!urlSigner.verify(reportId, expires, signature, Instant.now())) {
            throw new ResourceNotFoundException("This download link is invalid or has expired");
        }
        Report report = reportRepository.findById(reportId)
                .filter(item -> item.getStatus() == ReportStatus.READY && item.getStorageKey() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        try {
            return storage.get(report.getStorageKey());
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Report file not found");
        }
    }

    public String fileName(Long auditId) {
        return "seo-report-" + auditId + ".pdf";
    }

    // ---- shared with the worker --------------------------------------------

    record CreatedShare(ReportShare share, String url) {
    }

    @Transactional
    CreatedShare createShareInternal(Long auditId, Long userId, Duration ttl) {
        String token = SecureTokens.generate();
        ReportShare share = shareRepository.save(ReportShare.builder()
                .auditId(auditId)
                .tokenHash(SecureTokens.sha256Hex(token))
                .createdBy(userId)
                .expiresAt(Instant.now().plus(ttl))
                .build());
        return new CreatedShare(share, authProperties.getAppBaseUrl() + "/r/" + token);
    }

    Report newReport(Audit audit, Long organizationId, Long userId, String emailTo) {
        Flags flags = flags(organizationId);
        return Report.builder()
                .auditId(audit.getId())
                .organizationId(organizationId)
                .status(ReportStatus.PENDING)
                .whiteLabel(flags.whiteLabel())
                .watermark(flags.watermark())
                .emailTo(emailTo)
                .requestedBy(userId)
                .attempts(0)
                .build();
    }

    byte[] render(Audit audit, boolean whiteLabel, boolean watermark) {
        try {
            return pdfRenderer.render(htmlRenderer.render(modelFactory.build(audit, whiteLabel, watermark)));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not render the PDF", ex);
        }
    }

    void store(Report report, Audit audit, byte[] pdf) {
        String key = "reports/" + report.getOrganizationId() + "/" + audit.getId() + "/"
                + report.getId() + "-" + SecureTokens.generate().substring(0, 12) + ".pdf";
        try {
            storage.put(key, pdf);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store the PDF", ex);
        }
        report.setStorageKey(key);
        report.setSizeBytes((long) pdf.length);
        report.setCompletedAt(Instant.now());
    }

    private record Flags(boolean whiteLabel, boolean watermark) {
    }

    private Flags flags(Long organizationId) {
        PlanLimits limits = entitlementService.limitsFor(organizationId);
        boolean free = "FREE".equals(entitlementService.planCode(organizationId));
        return new Flags(limits.whiteLabel(), free);
    }

    private ReportShare requireActiveShare(String token) {
        if (token == null || token.length() < 20 || token.length() > 100) {
            throw new ResourceNotFoundException("Report not found");
        }
        return shareRepository.findByTokenHash(SecureTokens.sha256Hex(token))
                .filter(share -> share.isActive(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private Audit requireAudit(Long auditId) {
        return auditRepository.findByIdWithWebsite(auditId)
                .filter(audit -> audit.getStatus() == AuditStatus.COMPLETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private Audit requireCompletedAudit(Long projectId, Long auditId, Long userId) {
        Audit audit = projectAccessService.requireOwnedAudit(projectId, auditId, userId);
        if (audit.getStatus() != AuditStatus.COMPLETED) {
            throw new InvalidStateException("Reports are available once the audit has completed");
        }
        return audit;
    }

    private static Long organizationId(Audit audit) {
        return audit.getWebsite().getProject().getOrganization().getId();
    }
}
