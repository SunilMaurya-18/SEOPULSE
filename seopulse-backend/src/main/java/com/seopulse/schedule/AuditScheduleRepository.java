package com.seopulse.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditScheduleRepository extends JpaRepository<AuditSchedule, Long> {

    Optional<AuditSchedule> findByWebsiteId(Long websiteId);

    List<AuditSchedule> findByWebsiteIdIn(List<Long> websiteIds);

    /** Claims one due schedule; concurrent dispatchers skip rows another one holds. */
    @Query(value = """
        SELECT * FROM audit_schedules
        WHERE enabled AND next_run_at <= :now
        ORDER BY next_run_at
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<AuditSchedule> lockNextDue(@Param("now") Instant now);
}
