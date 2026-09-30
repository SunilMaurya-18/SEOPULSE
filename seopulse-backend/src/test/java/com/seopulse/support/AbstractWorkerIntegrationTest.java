package com.seopulse.support;

import com.seopulse.organization.service.OrganizationProvisioningService;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.user.entity.Role;
import com.seopulse.user.entity.User;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import com.seopulse.website.seo.service.AuditAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Runs the audit worker (stream consumers, outbox publisher) against a
 * local test website. Private networks are allowed so the crawler can
 * reach it.
 */
@ActiveProfiles("worker")
public abstract class AbstractWorkerIntegrationTest extends AbstractIntegrationTest {

    protected static final TestSite SITE = TestSite.start();

    @MockitoSpyBean
    protected AuditAnalysisService analysisService;

    @Autowired
    protected AuditRepository auditRepository;

    @Autowired
    protected WebsiteRepository websiteRepository;

    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected OrganizationProvisioningService organizationProvisioningService;

    @DynamicPropertySource
    static void crawlerProperties(DynamicPropertyRegistry registry) {
        registry.add("seopulse.crawler.allow-private-networks", () -> "true");
        registry.add("seopulse.crawler.allowed-ports", () -> "80,443," + SITE.port());
        registry.add("seopulse.crawler.min-delay-ms", () -> "0");
        registry.add("seopulse.crawler.max-duration-minutes", () -> "1");
        registry.add("seopulse.crawler.request-timeout-ms", () -> "5000");
        registry.add("seopulse.worker.outbox-publish-interval-ms", () -> "200");
    }

    protected Website createWebsiteFor(String url) {
        User owner = userRepository.save(User.builder()
                .name("Worker test")
                .email(uniqueEmail("worker"))
                .password("unused")
                .role(Role.USER)
                .emailVerifiedAt(Instant.now())
                .build());

        Project project = projectRepository.save(Project.builder()
                .name("Worker project")
                .user(owner)
                .organization(organizationProvisioningService.ensureFor(owner))
                .build());

        return websiteRepository.save(Website.builder()
                .name("Site " + UUID.randomUUID())
                .url(url)
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build());
    }

    /** Inserted directly, without an outbox row, so no consumer picks it up. */
    protected Audit queuedAudit(Website website) {
        return auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.QUEUED)
                .build());
    }

    protected Audit awaitAudit(Long auditId, Predicate<Audit> condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        Audit audit = auditRepository.findById(auditId).orElseThrow();
        while (!condition.test(audit)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for audit " + auditId + "; status=" + audit.getStatus()
                        + ", error=" + audit.getErrorMessage());
            }
            Thread.sleep(100);
            audit = auditRepository.findById(auditId).orElseThrow();
        }
        return audit;
    }
}
