package com.seopulse.project.dto;

public record ProjectSummaryResponse(
        Long projectId,
        String projectName,
        long totalWebsites,
        long totalAudits,
        long completedAudits,
        long failedAudits,
        long activeAudits
) {
}
