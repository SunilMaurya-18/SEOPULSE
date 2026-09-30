package com.seopulse.common.security;

import com.seopulse.common.exception.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes problem responses from servlet filters and security handlers,
 * which run outside Spring MVC's exception handling.
 */
public final class ProblemResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ProblemResponseWriter() {
    }

    public static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String detail,
            Map<String, String> headers
    ) throws IOException {

        ProblemDetail problem = Problems.of(status, detail, request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        headers.forEach(response::setHeader);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(toMap(problem)));
    }

    /** Same flat shape Spring MVC produces (custom properties at the top level). */
    static Map<String, Object> toMap(ProblemDetail problem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType() == null ? "about:blank" : problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        if (problem.getInstance() != null) {
            body.put("instance", problem.getInstance().toString());
        }
        if (problem.getProperties() != null) {
            body.putAll(problem.getProperties());
        }
        return body;
    }
}
