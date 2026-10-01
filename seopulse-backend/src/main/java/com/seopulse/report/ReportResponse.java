package com.seopulse.report;

import java.time.Instant;

public record ReportResponse(
        Long id,
        Long auditId,
        ReportStatus status,
        Long sizeBytes,
        boolean whiteLabel,
        boolean watermark,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {

    static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getAuditId(),
                report.getStatus(),
                report.getSizeBytes(),
                report.isWhiteLabel(),
                report.isWatermark(),
                report.getErrorMessage(),
                report.getCreatedAt(),
                report.getCompletedAt()
        );
    }
}
