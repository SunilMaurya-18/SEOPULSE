-- Razorpay (INR) subscriptions alongside Stripe. A workspace pays through one provider at a time.
ALTER TABLE subscriptions
    ADD COLUMN razorpay_subscription_id VARCHAR(255),
    ADD COLUMN billing_provider         VARCHAR(20);

CREATE UNIQUE INDEX ux_subscriptions_razorpay ON subscriptions (razorpay_subscription_id)
    WHERE razorpay_subscription_id IS NOT NULL;

UPDATE subscriptions SET billing_provider = 'STRIPE' WHERE stripe_subscription_id IS NOT NULL;
