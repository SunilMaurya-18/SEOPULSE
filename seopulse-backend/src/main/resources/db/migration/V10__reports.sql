-- Phase 5: server-side PDF reports, shareable read-only links, white-label branding.

CREATE TABLE reports
(
    id              BIGSERIAL PRIMARY KEY,
    audit_id        BIGINT                   NOT NULL,
    organization_id BIGINT                   NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    storage_key     VARCHAR(500),
    size_bytes      BIGINT,
    white_label     BOOLEAN                  NOT NULL DEFAULT FALSE,
    watermark       BOOLEAN                  NOT NULL DEFAULT FALSE,
    -- Comma-separated addresses to email once the PDF is ready (scheduled audits).
    email_to        VARCHAR(1000),
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    error_message   VARCHAR(500),
    requested_by    BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_reports_audit
        FOREIGN KEY (audit_id) REFERENCES audits (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_user
        FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_reports_status CHECK (status IN ('PENDING', 'GENERATING', 'READY', 'FAILED'))
);

CREATE INDEX idx_reports_audit ON reports (audit_id, created_at);
CREATE INDEX idx_reports_pending ON reports (created_at) WHERE status = 'PENDING';


CREATE TABLE report_shares
(
    id             BIGSERIAL PRIMARY KEY,
    audit_id       BIGINT                   NOT NULL,
    token_hash     VARCHAR(64)              NOT NULL,
    created_by     BIGINT,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at     TIMESTAMP WITH TIME ZONE,
    view_count     INTEGER                  NOT NULL DEFAULT 0,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_report_shares_token UNIQUE (token_hash),
    CONSTRAINT fk_report_shares_audit
        FOREIGN KEY (audit_id) REFERENCES audits (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_shares_user
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_report_shares_audit ON report_shares (audit_id);


-- {"companyName": "...", "brandColor": "#RRGGBB", "coverText": "...", "logoDataUrl": "data:image/png;base64,..."}
ALTER TABLE organizations
    ADD COLUMN branding JSONB;
