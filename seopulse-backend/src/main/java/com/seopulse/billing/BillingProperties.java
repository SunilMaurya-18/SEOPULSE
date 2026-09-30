package com.seopulse.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seopulse.billing")
@Getter
@Setter
public class BillingProperties {

    private String stripeSecretKey = "";
    private String stripeWebhookSecret = "";
    private String proMonthlyPriceId = "";
    private String proYearlyPriceId = "";
    private String agencyMonthlyPriceId = "";
    private String agencyYearlyPriceId = "";
    private String successUrl = "http://localhost:5173/settings?billing=success";
    private String cancelUrl = "http://localhost:5173/pricing";

    public boolean stripeEnabled() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    public String priceId(String plan, String interval) {
        boolean yearly = "year".equalsIgnoreCase(interval);
        if ("AGENCY".equalsIgnoreCase(plan)) {
            return yearly ? agencyYearlyPriceId : agencyMonthlyPriceId;
        }
        return yearly ? proYearlyPriceId : proMonthlyPriceId;
    }
}
