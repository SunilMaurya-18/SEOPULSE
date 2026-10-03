package com.seopulse.abuse;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "seopulse.abuse")
@Getter
@Setter
public class AbuseProperties {

    private boolean blockDisposableEmails = true;

    /** Added to the bundled list of throwaway email domains. */
    private List<String> extraBlockedDomains = new ArrayList<>();

    /** Cloudflare Turnstile secret; CAPTCHA checks are skipped while it is blank. */
    private String turnstileSecretKey = "";

    private String turnstileVerifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private Duration turnstileTimeout = Duration.ofSeconds(5);
}
