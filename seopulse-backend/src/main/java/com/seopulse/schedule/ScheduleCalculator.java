package com.seopulse.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ThreadLocalRandom;

public final class ScheduleCalculator {

    /** Spreads runs that share an hour so they don't all hit the queue at once. */
    static final Duration MAX_JITTER = Duration.ofMinutes(30);

    private ScheduleCalculator() {
    }

    /** The first scheduled slot strictly after {@code after}, without jitter. */
    public static Instant nextRun(ScheduleFrequency frequency, Integer dayOfWeek, int hourOfDay, ZoneId zone, Instant after) {

        ZonedDateTime now = after.atZone(zone);
        ZonedDateTime candidate = now.withHour(hourOfDay).withMinute(0).withSecond(0).withNano(0);

        if (frequency == ScheduleFrequency.WEEKLY) {
            DayOfWeek day = DayOfWeek.of(dayOfWeek == null ? 1 : dayOfWeek);
            candidate = candidate.with(TemporalAdjusters.nextOrSame(day));
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusWeeks(1);
            }
        } else if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }

        return candidate.toInstant();
    }

    public static Instant withJitter(Instant slot) {
        return slot.plusSeconds(ThreadLocalRandom.current().nextLong(MAX_JITTER.toSeconds() + 1));
    }
}
