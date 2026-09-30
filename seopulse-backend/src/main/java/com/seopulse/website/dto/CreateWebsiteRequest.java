package com.seopulse.website.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWebsiteRequest(
        @NotBlank(message = "Website name is required")
        @Size(max = 100, message = "Website name must not exceed 100 characters")
        String name,
        @NotBlank(message = "Website URL is required")
        @Size(max = 2048, message = "Website url must not exceed 2048 characters")
        String url
) {

}
