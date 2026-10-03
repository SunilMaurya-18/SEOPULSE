package com.seopulse.account;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Everything SEOPulse holds about a user, as returned by the data export. */
public record AccountExport(
        Instant exportedAt,
        Profile profile,
        List<Workspace> workspaces,
        Newsletter newsletter
) {

    public record Profile(
            Long id,
            String name,
            String email,
            Instant createdAt,
            Instant emailVerifiedAt,
            String termsAcceptedVersion,
            Instant termsAcceptedAt
    ) {
    }

    public record Workspace(
            Long id,
            String name,
            String role,
            String plan,
            Instant joinedAt,
            List<Project> projects
    ) {
    }

    public record Project(
            Long id,
            String name,
            String description,
            Instant createdAt,
            List<Website> websites
    ) {
    }

    public record Website(
            Long id,
            String name,
            String url,
            String status,
            Instant createdAt,
            List<Audit> audits
    ) {
    }

    public record Audit(
            Long id,
            String status,
            String triggeredBy,
            Integer score,
            Map<String, Integer> categoryScores,
            Integer pagesCrawled,
            Integer issueCount,
            Integer errorCount,
            Integer warningCount,
            Integer infoCount,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt
    ) {
    }

    public record Newsletter(
            boolean subscribed,
            Instant consentAt,
            Instant confirmedAt,
            Instant unsubscribedAt
    ) {
    }
}
