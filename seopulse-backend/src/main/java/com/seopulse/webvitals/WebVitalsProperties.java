package com.seopulse.webvitals;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seopulse.web-vitals")
@Getter
@Setter
public class WebVitalsProperties {

    private boolean enabled = true;

    /** Google Cloud API key. Works without one, but Google's anonymous quota is very small. */
    private String apiKey = "";

    private String apiUrl = "https://www.googleapis.com/pagespeedonline/v5/runPagespeed";

    /** "mobile" or "desktop". Google ranks on mobile, so that is the default. */
    private String strategy = "mobile";

    private Duration timeout = Duration.ofSeconds(90);

    private int maxConcurrent = 4;

    /** A PENDING row older than this is reported as failed (the worker died mid-measurement). */
    private Duration staleAfter = Duration.ofMinutes(10);
}
