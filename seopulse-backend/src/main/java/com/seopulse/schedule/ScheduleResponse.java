package com.seopulse.schedule;

import java.time.Instant;

/**
 * @param configured   false when the website has no schedule yet
 * @param planSchedule the most frequent schedule the plan allows: NONE, WEEKLY or DAILY
 */
public record ScheduleResponse(
        Long websiteId,
        boolean configured,
        String planSchedule,
        ScheduleFrequency frequency,
        Integer dayOfWeek,
        Integer hourOfDay,
        String timezone,
        boolean enabled,
        Instant nextRunAt,
        Instant lastRunAt,
        Long lastAuditId,
        String lastError
) {

    static ScheduleResponse none(Long websiteId, String planSchedule) {
        return new ScheduleResponse(websiteId, false, planSchedule, null, null, null, null,
                false, null, null, null, null);
    }

    static ScheduleResponse from(AuditSchedule schedule, String planSchedule) {
        return new ScheduleResponse(
                schedule.getWebsite().getId(),
                true,
                planSchedule,
                schedule.getFrequency(),
                schedule.getDayOfWeek() == null ? null : schedule.getDayOfWeek().intValue(),
                schedule.getHourOfDay() == null ? null : schedule.getHourOfDay().intValue(),
                schedule.getTimezone(),
                schedule.isEnabled(),
                schedule.getNextRunAt(),
                schedule.getLastRunAt(),
                schedule.getLastAuditId(),
                schedule.getLastError()
        );
    }
}
