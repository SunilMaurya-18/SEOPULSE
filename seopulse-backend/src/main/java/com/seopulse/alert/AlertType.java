package com.seopulse.alert;

public enum AlertType {
    /** The score fell by at least {@code threshold} points since the previous audit. */
    SCORE_DROP,
    /** At least {@code threshold} errors appeared that the previous audit did not have. */
    NEW_ERRORS,
    /** The start page failed or returned an error status. */
    PAGE_UNREACHABLE,
    AUDIT_FAILED
}
