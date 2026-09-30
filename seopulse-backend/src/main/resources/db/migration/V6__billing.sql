CREATE TABLE plans
(
    id                     BIGSERIAL PRIMARY KEY,
    code                   VARCHAR(20)  NOT NULL,
    name                   VARCHAR(50)  NOT NULL,
    limits                 JSONB        NOT NULL,
    stripe_monthly_price_id VARCHAR(255),
    stripe_yearly_price_id  VARCHAR(255),
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_plans_code UNIQUE (code)
);

INSERT INTO plans (code, name, limits, active)
VALUES ('FREE', 'Free',
        '{"websites":1,"pagesPerAudit":100,"auditsPerMonth":5,"members":1}',
        TRUE),
       ('PRO', 'Pro',
        '{"websites":10,"pagesPerAudit":2000,"auditsPerMonth":100,"members":3}',
        TRUE),
       ('AGENCY', 'Agency',
        '{"websites":50,"pagesPerAudit":10000,"auditsPerMonth":1000,"members":15}',
        TRUE);

CREATE TABLE subscriptions
(
    id                     BIGSERIAL PRIMARY KEY,
    organization_id        BIGINT                   NOT NULL,
    plan_id                BIGINT                   NOT NULL,
    stripe_subscription_id VARCHAR(255),
    status                 VARCHAR(30)              NOT NULL,
    current_period_end     TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end   BOOLEAN                  NOT NULL DEFAULT FALSE,
    trial_end              TIMESTAMP WITH TIME ZONE,
    grace_until            TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_subscriptions_org UNIQUE (organization_id),
    CONSTRAINT fk_subscriptions_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan
        FOREIGN KEY (plan_id) REFERENCES plans (id)
);

INSERT INTO subscriptions (organization_id, plan_id, status, created_at, updated_at)
SELECT o.id, p.id, 'ACTIVE', NOW(), NOW()
FROM organizations o
         CROSS JOIN plans p
WHERE p.code = 'FREE';

CREATE TABLE stripe_events
(
    id           VARCHAR(255) PRIMARY KEY,
    type         VARCHAR(120)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE usage_counters
(
    id           BIGSERIAL PRIMARY KEY,
    organization_id BIGINT                   NOT NULL,
    meter        VARCHAR(40)              NOT NULL,
    period_start DATE                     NOT NULL,
    used         INTEGER                  NOT NULL DEFAULT 0,

    CONSTRAINT uk_usage_counters UNIQUE (organization_id, meter, period_start),
    CONSTRAINT fk_usage_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);
