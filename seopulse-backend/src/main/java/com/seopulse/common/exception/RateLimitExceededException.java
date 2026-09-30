package com.seopulse.common.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long limit;
    private final long retryAfterSeconds;

    public RateLimitExceededException(long limit, long retryAfterSeconds) {
        super("Too many requests. Try again in " + retryAfterSeconds + " seconds.");
        this.limit = limit;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
