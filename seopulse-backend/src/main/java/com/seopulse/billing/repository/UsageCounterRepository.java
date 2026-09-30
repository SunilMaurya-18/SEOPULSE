package com.seopulse.billing.repository;

import com.seopulse.billing.entity.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {

    Optional<UsageCounter> findByOrganizationIdAndMeterAndPeriodStart(
            Long organizationId,
            String meter,
            LocalDate periodStart
    );

    @Modifying
    @Query("""
            UPDATE UsageCounter c
               SET c.used = c.used + :amount
             WHERE c.organizationId = :orgId
               AND c.meter = :meter
               AND c.periodStart = :period
               AND c.used + :amount <= :limit
            """)
    int consume(
            @Param("orgId") Long organizationId,
            @Param("meter") String meter,
            @Param("period") LocalDate periodStart,
            @Param("amount") int amount,
            @Param("limit") int limit
    );
}
