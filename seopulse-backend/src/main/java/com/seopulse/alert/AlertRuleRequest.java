package com.seopulse.alert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertRuleRequest(
        @NotNull AlertType type,
        Integer threshold,
        @NotNull AlertChannel channel,
        @Size(max = 2048) String target,
        Long websiteId,
        Boolean enabled
) {
}
