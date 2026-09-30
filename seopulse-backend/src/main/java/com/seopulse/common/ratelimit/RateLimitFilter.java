package com.seopulse.common.ratelimit;

import com.seopulse.common.security.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registered as a plain servlet filter, so it runs after the Spring
 * Security chain and can key limits by the authenticated user.
 * Login and forgot-password limits need the request body and are applied
 * in {@code AuthController}.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern AUDIT_CREATE = Pattern.compile("^/api/v1/projects/[^/]+/audits/?$");

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.isEnabled()
                || "OPTIONS".equals(request.getMethod())
                || !path.startsWith("/api/")
                || path.equals("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean post = "POST".equals(request.getMethod());
        String userId = authenticatedUserId();
        String ip = ClientIp.of(request);

        if (post && path.equals("/api/v1/auth/register")
                && reject(request, response, rateLimiter.tryConsume("register", ip, properties.getRegister()))) {
            return;
        }

        if (post && userId != null && AUDIT_CREATE.matcher(path).matches()
                && reject(request, response, rateLimiter.tryConsume("audit-create", userId, properties.getAuditCreate()))) {
            return;
        }

        String identity = userId != null ? "user:" + userId : "ip:" + ip;
        RateLimiter.Decision general = rateLimiter.tryConsume("general", identity, properties.getGeneral());
        if (reject(request, response, general)) {
            return;
        }

        if (general.limit() >= 0) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(general.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(general.remaining()));
        }

        chain.doFilter(request, response);
    }

    private static boolean reject(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimiter.Decision decision
    ) throws IOException {

        if (decision.allowed()) {
            return false;
        }

        ProblemResponseWriter.write(
                request,
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Try again in " + decision.retryAfterSeconds() + " seconds.",
                Map.of(
                        HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()),
                        "X-RateLimit-Limit", String.valueOf(decision.limit()),
                        "X-RateLimit-Remaining", "0"
                )
        );
        return true;
    }

    private static String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
