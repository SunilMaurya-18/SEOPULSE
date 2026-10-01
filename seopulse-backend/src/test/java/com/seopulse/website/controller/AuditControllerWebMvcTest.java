package com.seopulse.website.controller;

import com.seopulse.common.config.SecurityConfig;
import com.seopulse.common.exception.EmailNotVerifiedException;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.RateLimitExceededException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.common.ratelimit.RateLimitProperties;
import com.seopulse.common.ratelimit.RateLimiter;
import com.seopulse.common.security.CurrentUserService;
import com.seopulse.website.dto.AuditResponse;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.AuditTrigger;
import com.seopulse.website.events.AuditEventStreamService;
import com.seopulse.website.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
@TestPropertySource(properties = "jwt.secret=test-only-jwt-secret-that-is-long-enough-for-hs256")
class AuditControllerWebMvcTest {

    private static final String AUDITS = "/api/v1/projects/7/audits";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private AuditEventStreamService auditEventStreamService;

    @MockitoBean
    private RateLimiter rateLimiter;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get(AUDITS + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void createsAuditForTheAuthenticatedUser() throws Exception {
        when(auditService.createAudit(7L, 3L, 42L)).thenReturn(audit(AuditStatus.QUEUED));

        mvc.perform(post(AUDITS).param("websiteId", "3").with(user42()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(auditService).createAudit(7L, 3L, 42L);
    }

    @Test
    void mapsNotFoundToProblem() throws Exception {
        when(auditService.getAudit(anyLong(), anyLong(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Audit not found"));

        mvc.perform(get(AUDITS + "/99").with(user42()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Audit not found"))
                .andExpect(jsonPath("$.instance").value(AUDITS + "/99"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void unverifiedEmailIsForbiddenWithCode() throws Exception {
        when(auditService.createAudit(anyLong(), anyLong(), anyLong())).thenThrow(new EmailNotVerifiedException());

        mvc.perform(post(AUDITS).param("websiteId", "3").with(user42()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void rejectsMalformedParameters() throws Exception {
        mvc.perform(post(AUDITS).param("websiteId", "abc").with(user42()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'websiteId'"));

        mvc.perform(post(AUDITS).with(user42()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void cancelReturnsUpdatedAuditOrConflict() throws Exception {
        when(auditService.cancelAudit(7L, 5L, 42L)).thenReturn(audit(AuditStatus.CANCELLED));

        mvc.perform(post(AUDITS + "/5/cancel").with(user42()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        when(auditService.cancelAudit(7L, 6L, 42L))
                .thenThrow(new InvalidStateException("Only queued or running audits can be cancelled"));

        mvc.perform(post(AUDITS + "/6/cancel").with(user42()))
                .andExpect(status().isConflict());
    }

    @Test
    void rateLimitErrorsCarryRetryHeaders() throws Exception {
        when(auditService.createAudit(anyLong(), anyLong(), anyLong()))
                .thenThrow(new RateLimitExceededException(10, 120));

        mvc.perform(post(AUDITS).param("websiteId", "3").with(user42()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"))
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    void echoesValidRequestIdAndReplacesInvalidOnes() throws Exception {
        when(auditService.getAudit(anyLong(), anyLong(), anyLong())).thenReturn(audit(AuditStatus.COMPLETED));

        mvc.perform(get(AUDITS + "/1").header("X-Request-Id", "client-req-0001").with(user42()))
                .andExpect(header().string("X-Request-Id", "client-req-0001"));

        mvc.perform(get(AUDITS + "/1").header("X-Request-Id", "bad id\r\ninjected").with(user42()))
                .andExpect(header().string("X-Request-Id", not("bad id\r\ninjected")));
    }

    private static RequestPostProcessor user42() {
        return jwt().jwt(token -> token.subject("42"));
    }

    private static AuditResponse audit(AuditStatus status) {
        return new AuditResponse(5L, 3L, "https://example.com", status, null, 0, 0,
                null, null, null, Instant.now(), AuditTrigger.MANUAL, null, null, null);
    }
}
