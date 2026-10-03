package com.seopulse.auth;

import com.seopulse.auth.repository.RefreshTokenRepository;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.user.entity.User;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.WebsiteRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final String REFRESH = "/api/v1/auth/refresh";

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Test
    void registerIssuesAccessTokenAndHttpOnlyRefreshCookie() throws Exception {

        String email = uniqueEmail("cookie");

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/register")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.emailVerificationRequired").value(true))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn()
                .getResponse();

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .contains("seopulse_refresh=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/auth");

        assertThat(emails.lastTo(email, "Verify")).isPresent();
    }

    @Test
    void weakPasswordsAreRejectedWithFieldError() throws Exception {

        mvc.perform(post("/api/v1/auth/register")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail("weak"), "short1")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.errors.password", containsString("at least 10")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void registerRefusesThrowawayEmailAddresses() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("someone-" + System.nanoTime() + "@mailinator.com", TEST_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("permanent email")));
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() throws Exception {

        TestUser user = registerUser("rotate");
        Cookie original = user.refreshCookie();

        MockHttpServletResponse first = mvc.perform(refresh(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn()
                .getResponse();

        Cookie rotated = first.getCookie("seopulse_refresh");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());

        // Replaying the consumed token is treated as theft: the whole family is revoked.
        mvc.perform(refresh(original)).andExpect(status().isUnauthorized());
        mvc.perform(refresh(rotated)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRequiresCustomHeader() throws Exception {

        TestUser user = registerUser("csrf");

        mvc.perform(post(REFRESH).cookie(user.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() throws Exception {

        TestUser user = registerUser("logout");

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/logout")
                        .header("X-Requested-With", "fetch")
                        .cookie(user.refreshCookie()))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse();

        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");

        mvc.perform(refresh(user.refreshCookie())).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllRevokesEverySession() throws Exception {

        TestUser user = registerUser("logout-all");
        Cookie secondSession = login(user.email(), user.password()).getCookie("seopulse_refresh");

        mvc.perform(authed(post("/api/v1/auth/logout-all"), user.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(refresh(user.refreshCookie())).andExpect(status().isUnauthorized());
        mvc.perform(refresh(secondSession)).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/logout-all")).andExpect(status().isUnauthorized());
    }

    @Test
    void auditsRequireVerifiedEmailAndVerificationLinkWorksOnce() throws Exception {

        TestUser user = registerUser("verify");
        long projectId = createProject(user.accessToken());
        long websiteId = createWebsite(projectId);

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits")
                        .param("websiteId", String.valueOf(websiteId)), user.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        String token = emails.lastTokenTo(user.email(), "Verify");

        mvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.id()).orElseThrow().isEmailVerified()).isTrue();

        mvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        // A verified user gets past the guard (the audit itself fails URL validation here).
        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits")
                        .param("websiteId", String.valueOf(websiteId)), user.accessToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationSendsNewLink() throws Exception {

        TestUser user = registerUser("resend");
        int before = emails.sentTo(user.email()).size();

        mvc.perform(authed(post("/api/v1/auth/resend-verification"), user.accessToken()))
                .andExpect(status().isAccepted());

        assertThat(emails.sentTo(user.email())).hasSize(before + 1);
    }

    @Test
    void passwordResetChangesPasswordAndRevokesSessions() throws Exception {

        TestUser user = registerUser("reset");

        mvc.perform(post("/api/v1/auth/forgot-password")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + user.email() + "\"}"))
                .andExpect(status().isAccepted());

        String token = emails.lastTokenTo(user.email(), "Reset");
        String newPassword = "a-brand-new-passphrase";

        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\", \"newPassword\": \"%s\"}".formatted(token, newPassword)))
                .andExpect(status().isNoContent());

        mvc.perform(refresh(user.refreshCookie())).andExpect(status().isUnauthorized());

        mvc.perform(loginRequest(user.email(), user.password(), randomIp()))
                .andExpect(status().isUnauthorized());
        mvc.perform(loginRequest(user.email(), newPassword, randomIp()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\", \"newPassword\": \"%s\"}".formatted(token, newPassword)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmails() throws Exception {

        String email = uniqueEmail("nobody");

        mvc.perform(post("/api/v1/auth/forgot-password")
                        .with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\"}"))
                .andExpect(status().isAccepted());

        assertThat(emails.sentTo(email)).isEmpty();
    }

    @Test
    void accountLocksAfterRepeatedFailures() throws Exception {

        TestUser user = registerUser("lockout");

        // A different address per attempt keeps the per-IP rate limit out of the way.
        for (int attempt = 1; attempt < 10; attempt++) {
            mvc.perform(loginRequest(user.email(), "wrong-password-" + attempt, randomIp()))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(loginRequest(user.email(), "wrong-password-10", randomIp()))
                .andExpect(status().isLocked())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        // Even the correct password is refused while locked.
        mvc.perform(loginRequest(user.email(), user.password(), randomIp()))
                .andExpect(status().isLocked());

        assertThat(emails.lastTo(user.email(), "locked")).isPresent();

        User entity = userRepository.findById(user.id()).orElseThrow();
        assertThat(entity.getLockedUntil()).isNotNull();
    }

    @Test
    void loginIsRateLimitedPerClient() throws Exception {

        TestUser user = registerUser("ratelimit");
        String ip = randomIp();

        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(loginRequest(user.email(), "wrong-password", ip))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(loginRequest(user.email(), user.password(), ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // Another client is unaffected.
        mvc.perform(loginRequest(user.email(), user.password(), randomIp()))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestsGetProblemJson() throws Exception {

        mvc.perform(get("/api/v1/projects").header("X-Request-Id", "test-request-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "test-request-123"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId").value("test-request-123"));
    }

    private MockHttpServletRequestBuilder refresh(Cookie cookie) {
        return post(REFRESH).header("X-Requested-With", "fetch").cookie(cookie);
    }

    private MockHttpServletRequestBuilder loginRequest(String email, String password, String ip) {
        return post("/api/v1/auth/login")
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
    }

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return mvc.perform(loginRequest(email, password, randomIp()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    private long createProject(String token) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Workspace\"}"), token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JSON.readTree(body).get("id").asLong();
    }

    private long createWebsite(long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        return websiteRepository.save(Website.builder()
                        .name("Site")
                        .url("https://unresolvable-" + UUID.randomUUID() + ".invalid")
                        .status(WebsiteStatus.ACTIVE)
                        .project(project)
                        .build())
                .getId();
    }

    private static String registerBody(String email, String password) {
        return """
                {"name": "Tester", "email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
