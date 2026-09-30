package com.seopulse.website.seo.entity;

import com.seopulse.website.entity.AuditPage;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "seo_issues",
        indexes = {
                @Index(
                        name = "idx_seo_issues_audit_page_id",
                        columnList = "audit_page_id"
                ),
                @Index(
                        name = "idx_seo_issues_rule_code",
                        columnList = "rule_code"
                ),
                @Index(
                        name = "idx_seo_issues_severity",
                        columnList = "severity"
                ),
                @Index(
                        name = "uk_seo_issues_page_rule",
                        columnList = "audit_page_id, rule_code",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_page_id", nullable = false)
    private AuditPage auditPage;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 1000)
    private String recommendations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}