package com.seopulse.saas;

import com.seopulse.billing.entity.Subscription;
import com.seopulse.billing.repository.PlanRepository;
import com.seopulse.billing.repository.SubscriptionRepository;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.retention.RetentionService;
import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5: plan-gated schedules and alerts, comparison, share links and retention.
 */
class RetentionAndReportsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuditPageRepository auditPageRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private JdbcTemplate jdbc;

    private TestUser user;
    private long projectId;
    private long orgId;
    private Website website;

    @BeforeEach
    void setUp() throws Exception {
        user = registerVerifiedUser("phase5");

        String body = mvc.perform(authed(post("/api/v1/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Phase 5\"}"),
                        user.accessToken()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        projectId = JSON.readTree(body).get("id").asLong();

        Project project = projectRepository.findById(projectId).orElseThrow();
        orgId = project.getOrganization().getId();
        website = websiteRepository.save(Website.builder()
                .name("Client site")
                .url("https://client-" + UUID.randomUUID() + ".example.com")
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build());
    }

    @Test
    void schedulesFollowThePlan() throws Exception {
        String weekly = "{\"frequency\": \"WEEKLY\", \"dayOfWeek\": 1, \"hourOfDay\": 6, \"timezone\": \"Europe/London\"}";
        String daily = "{\"frequency\": \"DAILY\", \"hourOfDay\": 6}";

        mvc.perform(authed(put(schedulePath()).contentType(MediaType.APPLICATION_JSON).content(weekly), user.accessToken()))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.meter").value("schedules"));

        switchPlan("PRO");

        mvc.perform(authed(put(schedulePath()).contentType(MediaType.APPLICATION_JSON).content(weekly), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.planSchedule").value("WEEKLY"))
                .andExpect(jsonPath("$.nextRunAt").isNotEmpty());

        mvc.perform(authed(put(schedulePath()).contentType(MediaType.APPLICATION_JSON).content(daily), user.accessToken()))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.upgradeTo").value("AGENCY"));

        mvc.perform(authed(delete(schedulePath()), user.accessToken())).andExpect(status().isNoContent());
        mvc.perform(authed(get(schedulePath()), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void webhookAlertsNeedAPaidPlan() throws Exception {
        String webhook = "{\"type\": \"SCORE_DROP\", \"threshold\": 5, \"channel\": \"WEBHOOK\", \"target\": \"https://hooks.example.com/x\"}";
        String email = "{\"type\": \"AUDIT_FAILED\", \"channel\": \"EMAIL\", \"target\": \"ops@example.com\"}";

        mvc.perform(authed(post(alertsPath()).contentType(MediaType.APPLICATION_JSON).content(webhook), user.accessToken()))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.meter").value("webhookAlerts"));

        mvc.perform(authed(post(alertsPath()).contentType(MediaType.APPLICATION_JSON).content(email), user.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.threshold").isEmpty());

        mvc.perform(authed(get(alertsPath()), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'AUDIT_FAILED')]", hasSize(1)));
    }

    @Test
    void compareUsesThePreviousCompletedAudit() throws Exception {
        Audit first = completedAudit(90);
        Audit second = completedAudit(78);

        mvc.perform(authed(get("/api/v1/projects/" + projectId + "/audits/" + second.getId() + "/compare"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baselineAuditId").value(first.getId()))
                .andExpect(jsonPath("$.scoreDelta").value(-12))
                .andExpect(jsonPath("$.detailsAvailable").value(true));

        mvc.perform(authed(get("/api/v1/projects/" + projectId + "/websites/" + website.getId() + "/trend"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].score").value(90))
                .andExpect(jsonPath("$[1].score").value(78));
    }

    @Test
    void revokedShareLinksReturnNotFound() throws Exception {
        Audit audit = completedAudit(81);

        String created = mvc.perform(authed(post(auditPath(audit) + "/shares"), user.accessToken()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = JSON.readTree(created).get("url").asText();
        long shareId = JSON.readTree(created).get("id").asLong();
        String token = url.substring(url.lastIndexOf("/r/") + 3);

        mvc.perform(get("/api/v1/public/reports/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(81))
                .andExpect(jsonPath("$.watermark").value(true));

        mvc.perform(authed(delete(auditPath(audit) + "/shares/" + shareId), user.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/public/reports/" + token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/public/reports/" + token + "/pdf")).andExpect(status().isNotFound());
    }

    @Test
    void retentionRemovesPagesButKeepsScores() {
        Audit old = completedAudit(64);
        auditPageRepository.save(AuditPage.builder()
                .audit(old)
                .url(website.getUrl() + "/")
                .status(AuditPageStatus.CRAWLED)
                .statusCode(200)
                .depth(0)
                .h1Count(1)
                .imageCount(0)
                .imagesWithoutAlt(0)
                .internalLinkCount(0)
                .externalLinkCount(0)
                .build());
        jdbc.update("UPDATE audits SET created_at = NOW() - INTERVAL '400 days' WHERE id = ?", old.getId());

        retentionService.purge(Instant.now());

        Audit after = auditRepository.findById(old.getId()).orElseThrow();
        assertThat(after.getDetailsPurgedAt()).isNotNull();
        assertThat(after.getScore()).isEqualTo(64);
        assertThat(auditPageRepository.countByAuditId(old.getId())).isZero();
    }

    private Audit completedAudit(int score) {
        return auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.COMPLETED)
                .score(score)
                .pagesCrawled(1)
                .completedAt(Instant.now())
                .build());
    }

    private void switchPlan(String code) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
        subscription.setPlan(planRepository.findByCode(code).orElseThrow());
        subscriptionRepository.save(subscription);
    }

    private String schedulePath() {
        return "/api/v1/projects/" + projectId + "/websites/" + website.getId() + "/schedule";
    }

    private String alertsPath() {
        return "/api/v1/orgs/" + orgId + "/alerts";
    }

    private String auditPath(Audit audit) {
        return "/api/v1/projects/" + projectId + "/audits/" + audit.getId();
    }
}
