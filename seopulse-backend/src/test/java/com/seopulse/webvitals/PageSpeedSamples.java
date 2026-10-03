package com.seopulse.webvitals;

final class PageSpeedSamples {

    static final String WITH_FIELD_DATA = """
            {
              "loadingExperience": {
                "overall_category": "AVERAGE",
                "metrics": {
                  "LARGEST_CONTENTFUL_PAINT_MS": {"percentile": 2900, "category": "AVERAGE"},
                  "CUMULATIVE_LAYOUT_SHIFT_SCORE": {"percentile": 5, "category": "FAST"},
                  "INTERACTION_TO_NEXT_PAINT": {"percentile": 180, "category": "FAST"}
                }
              },
              "lighthouseResult": {
                "categories": {"performance": {"score": 0.73}},
                "audits": {
                  "largest-contentful-paint": {"numericValue": 3120.4},
                  "cumulative-layout-shift": {"numericValue": 0.0412},
                  "total-blocking-time": {"numericValue": 250},
                  "first-contentful-paint": {"numericValue": 1400.6},
                  "speed-index": {"numericValue": 2800}
                }
              }
            }
            """;

    private PageSpeedSamples() {
    }
}
