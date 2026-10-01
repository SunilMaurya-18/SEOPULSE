package com.seopulse.organization.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * White-label settings for PDF and shared reports. The logo is stored inline
 * as a data URL so report rendering never fetches remote resources.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganizationBranding(
        String companyName,
        String brandColor,
        String coverText,
        String logoDataUrl
) {
}
