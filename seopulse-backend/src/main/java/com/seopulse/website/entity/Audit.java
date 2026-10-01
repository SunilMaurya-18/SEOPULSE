package com.seopulse.website.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
        name = "audits",
        indexes = {
                @Index(
                        name = "idx_audits_website_id",
                        columnList = "website_id"
                ),
                @Index(
                        name = "idx_audits_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_audits_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    @Column
    private Integer score;

    @Column(name = "pages_crawled", nullable = false)
    private Integer pagesCrawled;

    @Column(name = "pages_analyzed", nullable = false)
    private Integer pagesAnalyzed;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 20)
    private AuditTrigger triggeredBy;

    // Aggregates written at completion, so trends and summaries keep
    // working after retention removes the page-level rows.

    @Column(name = "issue_count")
    private Integer issueCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(name = "info_count")
    private Integer infoCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_scores", columnDefinition = "jsonb")
    private Map<String, Integer> categoryScores;

    @Column(name = "score_version")
    private Integer scoreVersion;

    @Column(name = "details_purged_at")
    private Instant detailsPurgedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;

        if (triggeredBy == null) {
            triggeredBy = AuditTrigger.MANUAL;
        }

        if (pagesCrawled == null) {
            pagesCrawled = 0;
        }

        if (pagesAnalyzed == null) {
            pagesAnalyzed = 0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
    }
}