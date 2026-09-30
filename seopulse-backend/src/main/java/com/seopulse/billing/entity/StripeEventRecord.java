package com.seopulse.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "stripe_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeEventRecord {

    @Id
    @Column(length = 255)
    private String id;

    @Column(nullable = false, length = 120)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
