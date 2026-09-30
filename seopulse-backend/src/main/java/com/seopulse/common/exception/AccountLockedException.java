package com.seopulse.common.exception;

import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public AccountLockedException(long retryAfterSeconds) {
        super("Account temporarily locked after too many failed sign-in attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
