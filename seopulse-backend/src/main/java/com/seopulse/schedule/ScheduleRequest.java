package com.seopulse.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleRequest(
        @NotNull ScheduleFrequency frequency,
        @Min(1) @Max(7) Integer dayOfWeek,
        @Min(0) @Max(23) int hourOfDay,
        @Size(max = 64) String timezone,
        Boolean enabled
) {
}
