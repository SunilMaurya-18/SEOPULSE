-- Phase 5: scheduled audits, alert rules and their delivery outbox, ShedLock.

ALTER TABLE audits
    ADD COLUMN triggered_by VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

CREATE TABLE audit_schedules
(
    id            BIGSERIAL PRIMARY KEY,
    website_id    BIGINT                   NOT NULL,
    frequency     VARCHAR(10)              NOT NULL,
    -- ISO day of week (1 = Monday); only used by WEEKLY schedules.
    day_of_week   SMALLINT,
    -- Local hour in the schedule's time zone.
    hour_of_day   SMALLINT                 NOT NULL,
    timezone      VARCHAR(64)              NOT NULL DEFAULT 'UTC',
    enabled       BOOLEAN                  NOT NULL DEFAULT TRUE,
    next_run_at   TIMESTAMP WITH TIME ZONE,
    last_run_at   TIMESTAMP WITH TIME ZONE,
    last_audit_id BIGINT,
    last_error    VARCHAR(500),
    created_by    BIGINT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_audit_schedules_website UNIQUE (website_id),
    CONSTRAINT fk_audit_schedules_website
        FOREIGN KEY (website_id) REFERENCES websites (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_schedules_user
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_audit_schedules_frequency CHECK (frequency IN ('DAILY', 'WEEKLY')),
    CONSTRAINT ck_audit_schedules_day CHECK (day_of_week IS NULL OR day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_audit_schedules_hour CHECK (hour_of_day BETWEEN 0 AND 23)
);

CREATE INDEX idx_audit_schedules_due
    ON audit_schedules (next_run_at)
    WHERE enabled;


CREATE TABLE alert_rules
(
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT                   NOT NULL,
    -- NULL applies the rule to every website in the organization.
    website_id      BIGINT,
    type            VARCHAR(30)              NOT NULL,
    threshold       INTEGER,
    channel         VARCHAR(20)              NOT NULL,
    -- Email address, Slack incoming-webhook URL or webhook URL.
    -- NULL for EMAIL means the organization's owners and admins.
    target          VARCHAR(2048),
    signing_secret  VARCHAR(128),
    enabled         BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_alert_rules_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rules_website
        FOREIGN KEY (website_id) REFERENCES websites (id) ON DELETE CASCADE,
    CONSTRAINT ck_alert_rules_type
        CHECK (type IN ('SCORE_DROP', 'NEW_ERRORS', 'PAGE_UNREACHABLE', 'AUDIT_FAILED')),
    CONSTRAINT ck_alert_rules_channel
        CHECK (channel IN ('EMAIL', 'SLACK_WEBHOOK', 'WEBHOOK'))
);

CREATE INDEX idx_alert_rules_org ON alert_rules (organization_id);
CREATE INDEX idx_alert_rules_website ON alert_rules (website_id);


CREATE TABLE alert_outbox
(
    id              BIGSERIAL PRIMARY KEY,
    alert_rule_id   BIGINT,
    organization_id BIGINT                   NOT NULL,
    audit_id        BIGINT,
    event_type      VARCHAR(30)              NOT NULL,
    channel         VARCHAR(20)              NOT NULL,
    target          VARCHAR(2048),
    subject         VARCHAR(200)             NOT NULL,
    payload         TEXT                     NOT NULL,
    delivered       BOOLEAN                  NOT NULL DEFAULT FALSE,
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_alert_outbox_rule
        FOREIGN KEY (alert_rule_id) REFERENCES alert_rules (id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_outbox_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_outbox_audit
        FOREIGN KEY (audit_id) REFERENCES audits (id) ON DELETE SET NULL
);

CREATE INDEX idx_alert_outbox_pending
    ON alert_outbox (next_attempt_at)
    WHERE NOT delivered;

CREATE INDEX idx_alert_outbox_org ON alert_outbox (organization_id, created_at);

-- One alert per rule and audit, even if completion is handled twice.
CREATE UNIQUE INDEX uk_alert_outbox_rule_audit
    ON alert_outbox (alert_rule_id, audit_id, event_type)
    WHERE alert_rule_id IS NOT NULL AND audit_id IS NOT NULL;


-- Default alerts are created once per organization, so deleting them sticks.
ALTER TABLE organizations
    ADD COLUMN alert_defaults_applied BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO alert_rules (organization_id, website_id, type, threshold, channel, target, enabled, created_at, updated_at)
SELECT o.id, NULL, d.type, d.threshold, 'EMAIL', NULL, TRUE, NOW(), NOW()
FROM organizations o
         CROSS JOIN (VALUES ('SCORE_DROP', 10), ('NEW_ERRORS', 1)) AS d (type, threshold)
WHERE EXISTS (SELECT 1
              FROM projects p
                       JOIN websites w ON w.project_id = p.id
              WHERE p.organization_id = o.id);

UPDATE organizations o
SET alert_defaults_applied = TRUE
WHERE EXISTS (SELECT 1 FROM alert_rules r WHERE r.organization_id = o.id);


-- Plan gates for Phase 5 features.
UPDATE plans
SET limits = limits || '{"schedule":"NONE","webhookAlerts":false,"whiteLabel":false,"retentionDays":30}'::jsonb
WHERE code = 'FREE';

UPDATE plans
SET limits = limits || '{"schedule":"WEEKLY","webhookAlerts":true,"whiteLabel":false,"retentionDays":180}'::jsonb
WHERE code = 'PRO';

UPDATE plans
SET limits = limits || '{"schedule":"DAILY","webhookAlerts":true,"whiteLabel":true,"retentionDays":365}'::jsonb
WHERE code = 'AGENCY';


-- ShedLock: each scheduled job runs on one instance at a time.
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
