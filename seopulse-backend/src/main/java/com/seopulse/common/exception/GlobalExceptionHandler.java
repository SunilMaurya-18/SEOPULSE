package com.seopulse.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps exceptions to RFC 7807 problem responses. Spring MVC's own
 * exceptions are handled by {@link ResponseEntityExceptionHandler}; every
 * body is enriched with {@code requestId} and {@code timestamp}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + exception.getName() + "'", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidState(
            InvalidStateException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ProblemDetail> handleEmailNotVerified(
            EmailNotVerifiedException exception,
            HttpServletRequest request
    ) {
        ProblemDetail body = Problems.of(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
        body.setProperty("code", "EMAIL_NOT_VERIFIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(
            AccountLockedException exception,
            HttpServletRequest request
    ) {
        ProblemDetail body = Problems.of(HttpStatus.LOCKED, exception.getMessage(), request.getRequestURI());
        body.setProperty("code", "ACCOUNT_LOCKED");
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ProblemDetail> handleWeakPassword(
            WeakPasswordException exception,
            HttpServletRequest request
    ) {
        ProblemDetail body = Problems.of(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        body.setProperty("errors", Map.of("password", exception.getMessage()));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(
            InvalidTokenException exception,
            HttpServletRequest request
    ) {
        ProblemDetail body = Problems.of(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
        body.setProperty("code", "INVALID_TOKEN");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .header("X-RateLimit-Limit", String.valueOf(exception.getLimit()))
                .header("X-RateLimit-Remaining", "0")
                .body(Problems.of(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception: method={}, path={}", request.getMethod(), request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        body.setProperty("errors", errors);

        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ProblemDetail problem) {
            String path = request instanceof ServletWebRequest servlet
                    ? servlet.getRequest().getRequestURI()
                    : null;
            Problems.enrich(problem, path);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(Problems.of(status, detail, request.getRequestURI()));
    }
}
