package com.seopulse.schedule;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleCalculatorTest {

    @Test
    void dailyRunsLaterTodayWhenTheHourHasNotPassed() {
        Instant now = Instant.parse("2026-10-01T05:30:00Z");

        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.DAILY, null, 9, ZoneOffset.UTC, now))
                .isEqualTo(Instant.parse("2026-10-01T09:00:00Z"));
    }

    @Test
    void dailyRunsTomorrowWhenTheHourHasPassedOrIsNow() {
        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.DAILY, null, 9, ZoneOffset.UTC, Instant.parse("2026-10-01T09:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-02T09:00:00Z"));
        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.DAILY, null, 9, ZoneOffset.UTC, Instant.parse("2026-10-01T22:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-02T09:00:00Z"));
    }

    @Test
    void weeklyRunsOnTheConfiguredDay() {
        // 2026-10-01 is a Thursday; day 1 is Monday.
        Instant now = Instant.parse("2026-10-01T12:00:00Z");

        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.WEEKLY, 1, 6, ZoneOffset.UTC, now))
                .isEqualTo(Instant.parse("2026-10-05T06:00:00Z"));
        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.WEEKLY, 4, 18, ZoneOffset.UTC, now))
                .isEqualTo(Instant.parse("2026-10-01T18:00:00Z"));
        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.WEEKLY, 4, 6, ZoneOffset.UTC, now))
                .isEqualTo(Instant.parse("2026-10-08T06:00:00Z"));
    }

    @Test
    void hourIsInTheScheduleTimeZone() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        // 09:00 IST is 03:30 UTC.
        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.DAILY, null, 9, kolkata, now))
                .isEqualTo(Instant.parse("2026-10-01T03:30:00Z"));
    }

    @Test
    void localHourIsKeptAcrossDaylightSavingChanges() {
        ZoneId newYork = ZoneId.of("America/New_York");
        // DST ends on 2026-11-01; 08:00 local is 12:00Z before and 13:00Z after.
        Instant beforeChange = Instant.parse("2026-10-31T13:00:00Z");

        assertThat(ScheduleCalculator.nextRun(ScheduleFrequency.DAILY, null, 8, newYork, beforeChange))
                .isEqualTo(Instant.parse("2026-11-01T13:00:00Z"));
    }

    @Test
    void jitterStaysWithinThirtyMinutes() {
        Instant slot = Instant.parse("2026-10-01T09:00:00Z");
        for (int i = 0; i < 200; i++) {
            Instant jittered = ScheduleCalculator.withJitter(slot);
            assertThat(Duration.between(slot, jittered)).isBetween(Duration.ZERO, Duration.ofMinutes(30));
        }
    }
}
