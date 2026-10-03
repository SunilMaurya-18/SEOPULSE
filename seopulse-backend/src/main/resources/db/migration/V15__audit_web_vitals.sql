-- Core Web Vitals for the audited homepage, measured by Google PageSpeed Insights.
CREATE TABLE audit_web_vitals
(
    audit_id          BIGINT PRIMARY KEY,
    status            VARCHAR(20)              NOT NULL,
    strategy          VARCHAR(10)              NOT NULL,
    url               VARCHAR(2048)            NOT NULL,
    performance_score INTEGER,
    -- Lab data from a Lighthouse run.
    lab_lcp_ms        INTEGER,
    lab_cls           NUMERIC(6, 3),
    lab_tbt_ms        INTEGER,
    lab_fcp_ms        INTEGER,
    lab_speed_index_ms INTEGER,
    -- Field data (75th percentile of real Chrome users), only for sites with enough traffic.
    field_lcp_ms      INTEGER,
    field_cls         NUMERIC(6, 3),
    field_inp_ms      INTEGER,
    field_category    VARCHAR(20),
    error_message     VARCHAR(500),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    measured_at       TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_audit_web_vitals_audit
        FOREIGN KEY (audit_id) REFERENCES audits (id) ON DELETE CASCADE,
    CONSTRAINT ck_audit_web_vitals_status CHECK (status IN ('PENDING', 'READY', 'FAILED'))
);
