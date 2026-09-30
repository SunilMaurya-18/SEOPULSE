package com.seopulse.common.exception;

import com.seopulse.common.web.RequestIdFilter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

/**
 * Builds RFC 7807 problem bodies. Every body carries {@code requestId} and
 * {@code timestamp} so a client-side error can be matched to server logs.
 */
public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return enrich(problem, path);
    }

    public static ProblemDetail enrich(ProblemDetail problem, String path) {

        if (path != null && problem.getInstance() == null) {
            problem.setInstance(URI.create(path));
        }

        problem.setProperty("timestamp", Instant.now().toString());

        String requestId = RequestIdFilter.current();
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }

        return problem;
    }
}
