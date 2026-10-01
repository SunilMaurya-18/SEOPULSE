package com.seopulse.website.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(
        name = "audit_pages",
        indexes = {
                @Index(
                        name = "idx_audit_pages_audit_id",
                        columnList = "audit_id"
                ),
                @Index(
                        name = "idx_audit_pages_audit_url",
                        columnList = "audit_id,url"
                ),
                @Index(
                        name = "idx_audit_pages_status_code",
                        columnList = "status_code"
                ),
                @Index(
                        name = "idx_audit_pages_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditPageStatus status;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(length = 500)
    private String title;
    @Column(name = "meta_description", length = 1000)
    private String metaDescription;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "word_count")
    private Integer wordCount;
    @Column(nullable = false)
    private Integer depth;
    @Column(name = "h1_count", nullable = false)
    private Integer h1Count;
    @Column(name = "image_count", nullable = false)
    private Integer imageCount;
    @Column(name = "images_without_alt", nullable = false)
    private Integer imagesWithoutAlt;
    @Column(name = "internal_link_count", nullable = false)
    private Integer internalLinkCount;
    @Column(name = "external_link_count", nullable = false)
    private Integer externalLinkCount;

    @Column(name = "final_url", length = 2048)
    private String finalUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redirect_chain", columnDefinition = "jsonb")
    private List<String> redirectChain;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signals", columnDefinition = "jsonb")
    private PageSignals signals;

    @Column(name = "crawled_at")
    private Instant crawledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (depth == null) {
            depth = 0;
        }

        if (status == null) {
            status = AuditPageStatus.QUEUED;
        }

        if (h1Count == null) {
            h1Count = 0;
        }

        if (imageCount == null) {
            imageCount = 0;
        }

        if (imagesWithoutAlt == null) {
            imagesWithoutAlt = 0;
        }

        if (internalLinkCount == null) {
            internalLinkCount = 0;
        }

        if (externalLinkCount == null) {
            externalLinkCount = 0;
        }
    }
}
