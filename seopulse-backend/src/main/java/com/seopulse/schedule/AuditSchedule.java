package com.seopulse.schedule;

import com.seopulse.website.entity.Website;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false, unique = true)
    private Website website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScheduleFrequency frequency;

    /** ISO day of week, 1 = Monday. Only used by WEEKLY schedules. */
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Column(name = "hour_of_day", nullable = false)
    private Short hourOfDay;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_audit_id")
    private Long lastAuditId;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
