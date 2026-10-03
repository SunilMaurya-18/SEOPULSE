package com.seopulse.onboarding;

import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OnboardingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    void progressFollowsWhatTheWorkspaceContains() throws Exception {
        TestUser user = registerVerifiedUser("onboarding");
        long projectId = createProject(user);

        mvc.perform(authed(get("/api/v1/projects/{id}/onboarding", projectId), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dismissed").value(false))
                .andExpect(jsonPath("$.steps[0].id").value("VERIFY_EMAIL"))
                .andExpect(jsonPath("$.steps[0].done").value(true))
                .andExpect(jsonPath("$.steps[1].done").value(false))
                .andExpect(jsonPath("$.steps[2].done").value(false));

        Project project = projectRepository.findById(projectId).orElseThrow();
        Website website = websiteRepository.save(Website.builder()
                .name("Onboarding site")
                .url("https://onboarding-" + UUID.randomUUID() + ".example.com")
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build());
        auditRepository.save(Audit.builder().website(website).status(AuditStatus.COMPLETED).build());

        mvc.perform(authed(get("/api/v1/projects/{id}/onboarding", projectId), user.accessToken()))
                .andExpect(jsonPath("$.steps[1].id").value("ADD_WEBSITE"))
                .andExpect(jsonPath("$.steps[1].done").value(true))
                .andExpect(jsonPath("$.steps[2].id").value("RUN_AUDIT"))
                .andExpect(jsonPath("$.steps[2].done").value(true))
                .andExpect(jsonPath("$.steps[3].done").value(false))
                .andExpect(jsonPath("$.steps[4].done").value(false));
    }

    @Test
    void dismissingIsRemembered() throws Exception {
        TestUser user = registerVerifiedUser("onboarding-dismiss");
        long projectId = createProject(user);

        mvc.perform(authed(post("/api/v1/onboarding/dismiss"), user.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(authed(get("/api/v1/projects/{id}/onboarding", projectId), user.accessToken()))
                .andExpect(jsonPath("$.dismissed").value(true));
    }

    @Test
    void otherUsersProjectsAreNotVisible() throws Exception {
        TestUser owner = registerVerifiedUser("onboarding-owner");
        TestUser stranger = registerVerifiedUser("onboarding-stranger");
        long projectId = createProject(owner);

        mvc.perform(authed(get("/api/v1/projects/{id}/onboarding", projectId), stranger.accessToken()))
                .andExpect(status().isNotFound());
    }

    private long createProject(TestUser user) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Onboarding\"}"),
                        user.accessToken()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JSON.readTree(body).get("id").asLong();
    }
}
