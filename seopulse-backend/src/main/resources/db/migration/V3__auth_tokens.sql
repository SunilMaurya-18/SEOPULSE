ALTER TABLE users
    ADD COLUMN email_verified_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN failed_login_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until       TIMESTAMP WITH TIME ZONE;


-- Refresh tokens rotate on every use. All tokens descended from one login
-- share a family_id, so reuse of a rotated token revokes the whole family.
CREATE TABLE refresh_tokens
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT                   NOT NULL,
    family_id    UUID                     NOT NULL,
    token_hash   VARCHAR(64)                 NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at      TIMESTAMP WITH TIME ZONE,
    revoked_at   TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens (family_id);


CREATE TABLE email_verification_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                   NOT NULL,
    token_hash VARCHAR(64)                 NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_user_id
    ON email_verification_tokens (user_id);


CREATE TABLE password_reset_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                   NOT NULL,
    token_hash VARCHAR(64)                 NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);


-- audits.status is VARCHAR(20), so CANCELLED needs no column change;
-- this constraint documents and enforces the allowed values.
ALTER TABLE audits
    ADD CONSTRAINT ck_audits_status
        CHECK (status IN ('QUEUED', 'CRAWLING', 'ANALYZING', 'COMPLETED', 'FAILED', 'CANCELLED'));
