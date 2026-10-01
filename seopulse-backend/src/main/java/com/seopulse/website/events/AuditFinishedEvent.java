package com.seopulse.website.events;

import com.seopulse.website.entity.AuditStatus;

/**
 * Published by the worker once an audit reaches COMPLETED or FAILED.
 * Listeners run on the worker thread and must not throw.
 */
public record AuditFinishedEvent(Long auditId, AuditStatus status) {
}
