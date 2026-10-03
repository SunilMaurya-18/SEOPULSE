package com.seopulse.account;

import com.seopulse.newsletter.NewsletterSubscriberRepository;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.organization.repository.OrganizationRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountDataIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private NewsletterSubscriberRepository newsletterRepository;

    @Test
    void newsletterUsesDoubleOptInAndCanUnsubscribe() throws Exception {
        String email = uniqueEmail("news");

        subscribe(email).andExpect(status().isAccepted());
        assertThat(newsletterRepository.findByEmail(email)).get()
                .satisfies(subscriber -> assertThat(subscriber.isActive()).isFalse());

        String token = emails.lastTokenTo(email, "Confirm your SEOPulse subscription");
        mvc.perform(post("/api/v1/public/newsletter/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());
        assertThat(newsletterRepository.findByEmail(email).orElseThrow().isActive()).isTrue();

        mvc.perform(post("/api/v1/public/newsletter/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());
        assertThat(newsletterRepository.findByEmail(email).orElseThrow().getUnsubscribedAt()).isNotNull();
    }

    @Test
    void newsletterRequiresConsentAndRejectsUnknownTokens() throws Exception {
        mvc.perform(post("/api/v1/public/newsletter/subscribe")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"consent\": false}".formatted(uniqueEmail("noconsent"))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/public/newsletter/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"unknown-token\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportIncludesProfileWorkspacesAndAudits() throws Exception {
        TestUser user = registerVerifiedUser("export");
        Website website = createWebsite(user);
        auditRepository.save(Audit.builder().website(website).status(AuditStatus.COMPLETED).score(77).build());

        mvc.perform(authed(get("/api/v1/account/export"), user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.profile.email").value(user.email()))
                .andExpect(jsonPath("$.workspaces[0].role").value("OWNER"))
                .andExpect(jsonPath("$.workspaces[0].plan").value("FREE"))
                .andExpect(jsonPath("$.workspaces[0].projects[0].websites[0].url").value(website.getUrl()))
                .andExpect(jsonPath("$.workspaces[0].projects[0].websites[0].audits[0].score").value(77));
    }

    @Test
    void deletionRequiresTheCurrentPassword() throws Exception {
        TestUser user = registerVerifiedUser("wrongpass");

        deleteAccount(user, "not-my-password").andExpect(status().isBadRequest());

        assertThat(userRepository.findById(user.id())).isPresent();
    }

    @Test
    void deletionRemovesSoloWorkspaceWithItsData() throws Exception {
        TestUser user = registerVerifiedUser("solo");
        Website website = createWebsite(user);
        Long projectId = website.getProject().getId();
        Long organizationId = website.getProject().getOrganization().getId();
        auditRepository.save(Audit.builder().website(website).status(AuditStatus.COMPLETED).score(60).build());
        subscribe(user.email()).andExpect(status().isAccepted());

        deleteAccount(user, user.password()).andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.id())).isEmpty();
        assertThat(organizationRepository.findById(organizationId)).isEmpty();
        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(websiteRepository.findById(website.getId())).isEmpty();
        assertThat(newsletterRepository.findByEmail(user.email())).isEmpty();
        assertThat(emails.lastTo(user.email(), "Your SEOPulse account was deleted")).isPresent();
    }

    @Test
    void deletingAMemberHandsTheirProjectsToTheOwner() throws Exception {
        TestUser owner = registerVerifiedUser("owner");
        TestUser member = registerVerifiedUser("member");
        Long organizationId = personalOrganizationId(owner);
        joinOrganization(member, organizationId);

        Project project = projectRepository.save(Project.builder()
                .name("Client work")
                .user(userRepository.findById(member.id()).orElseThrow())
                .organization(organizationRepository.findById(organizationId).orElseThrow())
                .build());

        deleteAccount(member, member.password()).andExpect(status().isNoContent());

        assertThat(projectRepository.findByIdAndUserId(project.getId(), owner.id())).isPresent();
        assertThat(organizationRepository.findById(organizationId)).isPresent();
    }

    @Test
    void lastOwnerOfASharedWorkspaceMustHandOverFirst() throws Exception {
        TestUser owner = registerVerifiedUser("soleowner");
        TestUser member = registerVerifiedUser("teammate");
        joinOrganization(member, personalOrganizationId(owner));

        deleteAccount(owner, owner.password())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("owner")));

        assertThat(userRepository.findById(owner.id())).isPresent();
    }

    @Test
    void deletingAWorkspaceWithProjectsRemovesThem() throws Exception {
        TestUser user = registerVerifiedUser("orgdelete");
        Website website = createWebsite(user);
        Long organizationId = website.getProject().getOrganization().getId();

        mvc.perform(authed(delete("/api/v1/orgs/" + organizationId), user.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(organizationRepository.findById(organizationId)).isEmpty();
        assertThat(websiteRepository.findById(website.getId())).isEmpty();
    }

    private ResultActions subscribe(String email) throws Exception {
        return mvc.perform(post("/api/v1/public/newsletter/subscribe")
                .with(fromIp(randomIp()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"consent\": true}".formatted(email)));
    }

    private ResultActions deleteAccount(TestUser user, String password) throws Exception {
        return mvc.perform(authed(post("/api/v1/account/delete"), user.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"%s\"}".formatted(password)));
    }

    private Long personalOrganizationId(TestUser user) throws Exception {
        String body = mvc.perform(authed(get("/api/v1/orgs"), user.accessToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JSON.readTree(body).get(0).get("id").asLong();
    }

    private void joinOrganization(TestUser user, Long organizationId) {
        memberRepository.save(OrganizationMember.builder()
                .organization(organizationRepository.findById(organizationId).orElseThrow())
                .user(userRepository.findById(user.id()).orElseThrow())
                .role(OrganizationRole.MEMBER)
                .build());
    }

    private Website createWebsite(TestUser user) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Site\"}"),
                        user.accessToken()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Project project = projectRepository.findById(JSON.readTree(body).get("id").asLong()).orElseThrow();
        return websiteRepository.save(Website.builder()
                .name("Client site")
                .url("https://site-" + UUID.randomUUID() + ".example.com")
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build());
    }
}
