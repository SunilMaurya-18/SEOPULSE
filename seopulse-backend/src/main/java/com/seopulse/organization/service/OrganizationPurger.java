package com.seopulse.organization.service;

import com.seopulse.billing.BillingService;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.report.ReportRepository;
import com.seopulse.report.storage.ReportStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;

/**
 * Deletes a workspace and everything in it. Members, invitations, the
 * subscription, alerts and reports go with the organization row through
 * ON DELETE CASCADE; projects do not, so they are removed first.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationPurger {

    private final BillingService billingService;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final ReportRepository reportRepository;
    private final ReportStorage reportStorage;

    @Transactional
    public void purge(Long organizationId) {
        billingService.cancelBeforeDeletion(organizationId);
        List<String> storedReports = reportRepository.findStorageKeysByOrganizationId(organizationId);

        projectRepository.deleteByOrganizationId(organizationId);
        organizationRepository.deleteById(organizationId);

        deleteFilesAfterCommit(organizationId, storedReports);
        log.info("Organization purged: organizationId={}", organizationId);
    }

    private void deleteFilesAfterCommit(Long organizationId, List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : keys) {
                    try {
                        reportStorage.delete(key);
                    } catch (IOException ex) {
                        log.warn("Could not delete report file: organizationId={}, error={}", organizationId, ex.getMessage());
                    }
                }
            }
        });
    }
}
