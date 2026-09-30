package com.seopulse.project.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
