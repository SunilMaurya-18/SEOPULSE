package com.seopulse.billing.razorpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "seopulse.billing.razorpay")
@Getter
@Setter
public class RazorpayProperties {

    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";

    /** Razorpay plan IDs (plan_...), created in the Razorpay dashboard with INR prices. */
    private String proMonthlyPlanId = "";
    private String proYearlyPlanId = "";
    private String agencyMonthlyPlanId = "";
    private String agencyYearlyPlanId = "";

    private String apiUrl = "https://api.razorpay.com/v1";
    private Duration timeout = Duration.ofSeconds(15);

    public boolean enabled() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    public String planId(String plan, String interval) {
        boolean yearly = "year".equalsIgnoreCase(interval);
        if ("AGENCY".equalsIgnoreCase(plan)) {
            return yearly ? agencyYearlyPlanId : agencyMonthlyPlanId;
        }
        if ("PRO".equalsIgnoreCase(plan)) {
            return yearly ? proYearlyPlanId : proMonthlyPlanId;
        }
        return "";
    }

    /** Maps a Razorpay plan ID back to our plan code. */
    public Optional<String> planCodeFor(String razorpayPlanId) {
        if (razorpayPlanId == null || razorpayPlanId.isBlank()) {
            return Optional.empty();
        }
        return Map.of(
                        "PRO_M", proMonthlyPlanId, "PRO_Y", proYearlyPlanId,
                        "AGENCY_M", agencyMonthlyPlanId, "AGENCY_Y", agencyYearlyPlanId)
                .entrySet().stream()
                .filter(entry -> razorpayPlanId.equals(entry.getValue()))
                .map(entry -> entry.getKey().substring(0, entry.getKey().indexOf('_')))
                .findFirst();
    }
}
