package com.seopulse.security;

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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A user must never be able to read or act on another user's
 * projects, websites or audits. Foreign resources return 404.
 */
class OwnershipIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AuditRepository auditRepository;

    private String ownerToken;
    private String otherToken;
    private long ownerProjectId;
    private long otherProjectId;
    private long ownerWebsiteId;
    private long ownerAuditId;

    @BeforeEach
    void setUp() throws Exception {

        ownerToken = register("owner");
        otherToken = register("other");

        ownerProjectId = createProject(ownerToken);
        otherProjectId = createProject(otherToken);

        Project ownerProject = projectRepository.findById(ownerProjectId).orElseThrow();

        // Saved directly: creating a website through the API resolves DNS.
        Website website = websiteRepository.save(
                Website.builder()
                        .name("Owner site")
                        .url("https://owner-" + UUID.randomUUID() + ".example.com")
                        .status(WebsiteStatus.ACTIVE)
                        .project(ownerProject)
                        .build()
        );
        ownerWebsiteId = website.getId();

        Audit audit = auditRepository.save(
                Audit.builder()
                        .website(website)
                        .status(AuditStatus.COMPLETED)
                        .score(90)
                        .build()
        );
        ownerAuditId = audit.getId();
    }

    @Test
    void ownerCanReadOwnAudit() throws Exception {

        mockMvc.perform(authed(get(auditPath(ownerProjectId, ownerAuditId)), ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(authed(get(auditPath(ownerProjectId, ownerAuditId) + "/summary"), ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void otherUserGetsNotFoundForForeignProject() throws Exception {

        String project = "/api/v1/projects/" + ownerProjectId;

        mockMvc.perform(authed(get(project), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(project + "/summary"), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(delete(project), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(project + "/websites"), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(project + "/websites/" + ownerWebsiteId), otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUserGetsNotFoundForForeignAudits() throws Exception {

        String audit = auditPath(ownerProjectId, ownerAuditId);

        mockMvc.perform(authed(get(audit), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(audit + "/summary"), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(audit + "/pages"), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(get(audit + "/issues"), otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authed(
                        get("/api/v1/projects/" + ownerProjectId + "/audits")
                                .param("websiteId", String.valueOf(ownerWebsiteId)),
                        otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUserCannotStartAuditOnForeignWebsite() throws Exception {

        mockMvc.perform(authed(
                        post("/api/v1/projects/" + ownerProjectId + "/audits")
                                .param("websiteId", String.valueOf(ownerWebsiteId)),
                        otherToken))
                .andExpect(status().isNotFound());

        // Pairing the caller's own project with a foreign website must not work either.
        mockMvc.perform(authed(
                        post("/api/v1/projects/" + otherProjectId + "/audits")
                                .param("websiteId", String.valueOf(ownerWebsiteId)),
                        otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUserCannotReachForeignAuditThroughOwnProject() throws Exception {

        mockMvc.perform(authed(get(auditPath(otherProjectId, ownerAuditId)), otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenWithEmailSubjectIsRejected() throws Exception {

        Instant now = Instant.now();

        String legacyToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        JwtClaimsSet.builder()
                                .subject("someone@example.com")
                                .claim("role", "USER")
                                .issuedAt(now)
                                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                                .build()
                )
        ).getTokenValue();

        mockMvc.perform(authed(get("/api/v1/projects"), legacyToken))
                .andExpect(status().isUnauthorized());
    }

    private String register(String prefix) throws Exception {

        String email = prefix + "-" + UUID.randomUUID() + "@example.com";

        String body = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "%s", "email": "%s", "password": "password123"}
                                        """.formatted(prefix, email))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("accessToken").asString();
    }

    private long createProject(String token) throws Exception {

        String body = mockMvc.perform(authed(
                        post("/api/v1/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "Workspace"}
                                        """),
                        token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("id").asLong();
    }

    private static String auditPath(long projectId, long auditId) {
        return "/api/v1/projects/" + projectId + "/audits/" + auditId;
    }

    private static MockHttpServletRequestBuilder authed(
            MockHttpServletRequestBuilder request,
            String token
    ) {
        return request.header("Authorization", "Bearer " + token);
    }
}
