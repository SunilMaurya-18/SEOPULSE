package com.seopulse.website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_outbox",
        indexes = {
                @Index(
                        name = "idx_audit_outbox_unpublished",
                        columnList = "published,created_at"
                ),
                @Index(
                        name = "idx_audit_outbox_audit_id",
                        columnList = "audit_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AuditOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
