package com.seopulse.website.service;

/**
 * The audit left the expected status while it was being processed,
 * typically because the user cancelled it. Not a failure: no retry.
 */
public class AuditStateChangedException extends RuntimeException {

    public AuditStateChangedException(Long auditId, String expectedStatus) {
        super("Audit " + auditId + " is no longer " + expectedStatus);
    }
}
