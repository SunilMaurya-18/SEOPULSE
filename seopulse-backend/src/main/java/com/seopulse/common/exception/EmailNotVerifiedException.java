package com.seopulse.common.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Verify your email address before starting audits");
    }
}
