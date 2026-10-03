package com.seopulse.webvitals;

import java.math.BigDecimal;
import java.time.Instant;

public record WebVitalsResponse(
        State state,
        String strategy,
        String url,
        Integer performanceScore,
        Lab lab,
        Field field,
        String errorMessage,
        Instant measuredAt
) {

    public enum State {
        /** Measurement is running. */
        PENDING,
        READY,
        FAILED,
        /** Not measured: the feature is off, or the audit predates it. */
        UNAVAILABLE
    }

    public record Lab(Integer lcpMs, BigDecimal cls, Integer tbtMs, Integer fcpMs, Integer speedIndexMs) {
    }

    /** 75th percentile of real Chrome users over the last 28 days. */
    public record Field(Integer lcpMs, BigDecimal cls, Integer inpMs, String category) {
    }

    static WebVitalsResponse of(State state) {
        return new WebVitalsResponse(state, null, null, null, null, null, null, null);
    }
}
