package com.seopulse.report;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "white_label", nullable = false)
    private boolean whiteLabel;

    @Column(nullable = false)
    private boolean watermark;

    /** Comma-separated recipients to email once the PDF is ready. */
    @Column(name = "email_to", length = 1000)
    private String emailTo;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
