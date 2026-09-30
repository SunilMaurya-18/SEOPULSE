package com.seopulse.website.entity;

public enum AuditStatus {
    QUEUED,
    CRAWLING,
    ANALYZING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isActive() {
        return this == QUEUED || this == CRAWLING || this == ANALYZING;
    }
}
