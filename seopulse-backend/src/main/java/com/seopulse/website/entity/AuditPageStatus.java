package com.seopulse.website.entity;

public enum AuditPageStatus {
    QUEUED,
    CRAWLING,
    CRAWLED,
    REDIRECT,
    SKIPPED_ROBOTS,
    TOO_LARGE,
    FAILED
}
