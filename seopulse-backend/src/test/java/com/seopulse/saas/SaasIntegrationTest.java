package com.seopulse.saas;

import com.seopulse.common.email.EmailMessage;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: personal organizations, plan limits and emailed audit reports.
 */
class SaasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AuditRepository auditRepository;

    private TestUser user;
    private long projectId;
    private Website website;

    @BeforeEach
    void setUp() throws Exception {
        user = registerVerifiedUser("saas");

        String body = mvc.perform(authed(post("/api/v1/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Agency\"}"),
                        user.accessToken()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        projectId = JSON.readTree(body).get("id").asLong();

        Project project = projectRepository.findById(projectId).orElseThrow();
        // Saved directly: creating a website through the API resolves DNS.
        website = websiteRepository.save(Website.builder()
                .name("Client site")
                .url("https://client-" + UUID.randomUUID() + ".example.com")
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build());
    }

    @Test
    void registrationProvisionsPersonalOrganizationOnFreePlan() throws Exception {
        String orgs = mvc.perform(authed(get("/api/v1/orgs"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long orgId = JSON.readTree(orgs).get(0).get("id").asLong();

        mvc.perform(authed(get("/api/v1/orgs/" + orgId + "/billing"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.limits.websites").value(1))
                .andExpect(jsonPath("$.websitesUsed").value(1));

        mvc.perform(authed(get("/api/v1/orgs/" + orgId + "/members"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(user.email()))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void freePlanRejectsSecondWebsiteWithPaymentRequired() throws Exception {
        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/websites")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Second\", \"url\": \"https://second.example.com\"}"),
                        user.accessToken()))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT"))
                .andExpect(jsonPath("$.meter").value("websites"))
                .andExpect(jsonPath("$.limit").value(1));
    }

    @Test
    void completedReportIsEmailedToEachRecipientWithNote() throws Exception {
        Audit audit = auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.COMPLETED)
                .score(84)
                .build());

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + audit.getId() + "/email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"recipients": ["Client@Acme.io", "client@acme.io", "boss@acme.io"],
                                         "note": "Monthly health check"}
                                        """),
                        user.accessToken()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recipients").value(2));

        EmailMessage toClient = emails.lastTo("client@acme.io", "SEO audit report").orElseThrow();
        assertThat(toClient.body()).contains("Monthly health check").contains("/audits/" + audit.getId());
        assertThat(toClient.html()).contains("Open report");
        assertThat(emails.lastTo("boss@acme.io", "SEO audit report")).isPresent();
    }

    @Test
    void emailWithoutBodyGoesToTheSignedInUser() throws Exception {
        Audit audit = auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.COMPLETED)
                .score(70)
                .build());

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + audit.getId() + "/email"),
                        user.accessToken()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recipients").value(1));

        assertThat(emails.lastTo(user.email(), "SEO audit report")).isPresent();
    }

    @Test
    void unfinishedAuditCannotBeEmailed() throws Exception {
        Audit audit = auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.CRAWLING)
                .build());

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + audit.getId() + "/email"),
                        user.accessToken()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidRecipientIsRejected() throws Exception {
        Audit audit = auditRepository.save(Audit.builder()
                .website(website)
                .status(AuditStatus.COMPLETED)
                .score(90)
                .build());

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + audit.getId() + "/email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"recipients\": [\"not-an-email\"]}"),
                        user.accessToken()))
                .andExpect(status().isBadRequest());
    }
}
