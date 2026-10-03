package com.seopulse.admin;

import java.time.Instant;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record Stats(
            long users,
            long usersLast7Days,
            long usersLast30Days,
            long verifiedUsers,
            long organizations,
            long websites,
            long audits,
            long auditsLast24Hours,
            long failedAuditsLast24Hours,
            long activeAudits,
            long newsletterSubscribers,
            /** Plan code to number of workspaces on it (cancelled subscriptions excluded). */
            Map<String, Long> workspacesByPlan
    ) {
    }

    public record UserRow(
            Long id,
            String name,
            String email,
            String role,
            boolean emailVerified,
            boolean googleLinked,
            boolean locked,
            long workspaces,
            Instant createdAt
    ) {
    }

    public record OrganizationRow(
            Long id,
            String name,
            String plan,
            String subscriptionStatus,
            long members,
            long websites,
            long auditsThisMonth,
            Instant createdAt
    ) {
    }

    public record FailedAuditRow(
            Long id,
            String websiteUrl,
            Long organizationId,
            String organizationName,
            String errorMessage,
            int retryCount,
            Instant createdAt,
            Instant completedAt
    ) {
    }
}
