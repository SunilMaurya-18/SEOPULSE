package com.seopulse.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(
        @NotBlank(message = "Token is required")
        @Size(max = 200, message = "Invalid token")
        String token
) {
}
