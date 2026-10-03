package com.seopulse.webvitals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "audit_web_vitals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditWebVitals {

    public enum Status {
        PENDING,
        READY,
        FAILED
    }

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, length = 10)
    private String strategy;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "performance_score")
    private Integer performanceScore;

    @Column(name = "lab_lcp_ms")
    private Integer labLcpMs;

    @Column(name = "lab_cls", precision = 6, scale = 3)
    private BigDecimal labCls;

    @Column(name = "lab_tbt_ms")
    private Integer labTbtMs;

    @Column(name = "lab_fcp_ms")
    private Integer labFcpMs;

    @Column(name = "lab_speed_index_ms")
    private Integer labSpeedIndexMs;

    @Column(name = "field_lcp_ms")
    private Integer fieldLcpMs;

    @Column(name = "field_cls", precision = 6, scale = 3)
    private BigDecimal fieldCls;

    @Column(name = "field_inp_ms")
    private Integer fieldInpMs;

    @Column(name = "field_category", length = 20)
    private String fieldCategory;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "measured_at")
    private Instant measuredAt;
}
