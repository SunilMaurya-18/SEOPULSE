package com.seopulse.website.dto;

import com.seopulse.website.entity.WebsiteStatus;

import java.time.Instant;

public record WebsiteResponse(
        Long id,
        String name,
        String url,
        WebsiteStatus status,
        Long projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
