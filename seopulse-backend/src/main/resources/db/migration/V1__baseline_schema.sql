-- Baseline schema. Replaces the original V1-V10, which could not build
-- a database from scratch (the websites table was never created).

CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    email      VARCHAR(255)             NOT NULL,
    password   VARCHAR(255)             NOT NULL,
    role       VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_users_email UNIQUE (email)
);


CREATE TABLE projects
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150)             NOT NULL,
    description VARCHAR(500),
    user_id     BIGINT                   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_projects_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_projects_user_id
    ON projects (user_id);


CREATE TABLE websites
(
    id         BIGSERIAL PRIMARY KEY,
    url        VARCHAR(2048)            NOT NULL,
    name       VARCHAR(150)             NOT NULL,
    status     VARCHAR(20)              NOT NULL,
    project_id BIGINT                   NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_websites_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_websites_project_id
    ON websites (project_id);


CREATE TABLE audits
(
    id             BIGSERIAL PRIMARY KEY,
    website_id     BIGINT                   NOT NULL,
    status         VARCHAR(20)              NOT NULL,
    score          INTEGER,
    pages_crawled  INTEGER                  NOT NULL DEFAULT 0,
    pages_analyzed INTEGER                  NOT NULL DEFAULT 0,
    retry_count    INTEGER                  NOT NULL DEFAULT 0,
    max_retries    INTEGER                  NOT NULL DEFAULT 3,
    started_at     TIMESTAMP WITH TIME ZONE,
    completed_at   TIMESTAMP WITH TIME ZONE,
    error_message  VARCHAR(1000),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_audits_website
        FOREIGN KEY (website_id)
            REFERENCES websites (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_audits_website_id
    ON audits (website_id);

CREATE INDEX idx_audits_status
    ON audits (status);

CREATE INDEX idx_audits_created_at
    ON audits (created_at);

CREATE INDEX idx_audits_website_status
    ON audits (website_id, status);


CREATE TABLE audit_outbox
(
    id           BIGSERIAL PRIMARY KEY,
    audit_id     BIGINT                   NOT NULL,
    event_type   VARCHAR(50)              NOT NULL,
    published    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_audit_outbox_audit
        FOREIGN KEY (audit_id)
            REFERENCES audits (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_audit_outbox_unpublished
    ON audit_outbox (published, created_at);

CREATE INDEX idx_audit_outbox_audit_id
    ON audit_outbox (audit_id);


CREATE TABLE audit_pages
(
    id                  BIGSERIAL PRIMARY KEY,
    audit_id            BIGINT                   NOT NULL,
    url                 VARCHAR(2048)            NOT NULL,
    status              VARCHAR(20)              NOT NULL DEFAULT 'QUEUED',
    status_code         INTEGER,
    content_type        VARCHAR(100),
    title               VARCHAR(500),
    meta_description    VARCHAR(1000),
    canonical_url       VARCHAR(2048),
    word_count          INTEGER,
    depth               INTEGER                  NOT NULL DEFAULT 0,
    h1_count            INTEGER                  NOT NULL DEFAULT 0,
    image_count         INTEGER                  NOT NULL DEFAULT 0,
    images_without_alt  INTEGER                  NOT NULL DEFAULT 0,
    internal_link_count INTEGER                  NOT NULL DEFAULT 0,
    external_link_count INTEGER                  NOT NULL DEFAULT 0,
    crawled_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_audit_pages_audit
        FOREIGN KEY (audit_id)
            REFERENCES audits (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_audit_pages_audit_id
    ON audit_pages (audit_id);

CREATE INDEX idx_audit_pages_audit_url
    ON audit_pages (audit_id, url);

CREATE INDEX idx_audit_pages_status_code
    ON audit_pages (status_code);

CREATE INDEX idx_audit_pages_status
    ON audit_pages (status);


CREATE TABLE seo_issues
(
    id              BIGSERIAL PRIMARY KEY,
    audit_page_id   BIGINT                   NOT NULL,
    rule_code       VARCHAR(100)             NOT NULL,
    severity        VARCHAR(20)              NOT NULL,
    message         VARCHAR(500)             NOT NULL,
    recommendations VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_seo_issues_audit_page
        FOREIGN KEY (audit_page_id)
            REFERENCES audit_pages (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_seo_issues_audit_page_id
    ON seo_issues (audit_page_id);

CREATE INDEX idx_seo_issues_rule_code
    ON seo_issues (rule_code);

CREATE INDEX idx_seo_issues_severity
    ON seo_issues (severity);

CREATE UNIQUE INDEX uk_seo_issues_page_rule
    ON seo_issues (audit_page_id, rule_code);

CREATE INDEX idx_seo_issues_audit_page_severity
    ON seo_issues (audit_page_id, severity);
