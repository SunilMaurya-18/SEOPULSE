package com.seopulse.common.config;

import com.seopulse.common.web.RequestIdFilter;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sentry stays disabled until SENTRY_DSN is set. Events are tagged with the
 * request/audit IDs from the logging context and stripped of personal data.
 */
@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSend() {
        return (event, hint) -> {

            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                event.setTag("request_id", requestId);
            }

            String auditId = MDC.get("auditId");
            if (auditId != null) {
                event.setTag("audit_id", auditId);
            }

            Request request = event.getRequest();
            if (request != null) {
                request.setCookies(null);
                request.setData(null);
                request.setQueryString(null);
                if (request.getHeaders() != null) {
                    Map<String, String> safeHeaders = request.getHeaders().entrySet().stream()
                            .filter(header -> !isSensitiveHeader(header.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    request.setHeaders(safeHeaders);
                }
            }

            if (event.getUser() != null) {
                event.getUser().setEmail(null);
                event.getUser().setIpAddress(null);
                event.getUser().setUsername(null);
            }

            return event;
        };
    }

    private static boolean isSensitiveHeader(String name) {
        String lower = name.toLowerCase();
        return lower.equals("authorization")
                || lower.equals("cookie")
                || lower.equals("set-cookie")
                || lower.equals("x-forwarded-for");
    }
}
