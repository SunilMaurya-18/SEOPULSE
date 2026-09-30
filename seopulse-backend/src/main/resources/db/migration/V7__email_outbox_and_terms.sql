CREATE TABLE email_outbox
(
    id         BIGSERIAL PRIMARY KEY,
    to_address VARCHAR(255)             NOT NULL,
    subject    VARCHAR(200)             NOT NULL,
    body_text  TEXT                     NOT NULL,
    body_html  TEXT,
    sent       BOOLEAN                  NOT NULL DEFAULT FALSE,
    attempts   INTEGER                  NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_email_outbox_unsent ON email_outbox (sent, created_at);

ALTER TABLE users
    ADD COLUMN terms_accepted_version VARCHAR(40),
    ADD COLUMN terms_accepted_at TIMESTAMP WITH TIME ZONE;
