package com.seopulse.billing.repository;

import com.seopulse.billing.entity.StripeEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeEventRepository extends JpaRepository<StripeEventRecord, String> {
}
