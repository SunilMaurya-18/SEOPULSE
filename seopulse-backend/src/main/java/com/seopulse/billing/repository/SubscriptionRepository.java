package com.seopulse.billing.repository;

import com.seopulse.billing.entity.Subscription;
import com.seopulse.billing.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByOrganizationId(Long organizationId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    List<Subscription> findByStatusNot(SubscriptionStatus status);
}
