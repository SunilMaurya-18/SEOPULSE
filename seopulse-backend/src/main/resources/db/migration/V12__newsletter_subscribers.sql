-- Double opt-in mailing list. The token confirms the address and later
-- unsubscribes it, so it is stored as-is to build links in every email.
CREATE TABLE newsletter_subscribers
(
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255)             NOT NULL,
    token           VARCHAR(64)              NOT NULL,
    consent_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at    TIMESTAMP WITH TIME ZONE,
    unsubscribed_at TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_newsletter_subscribers_email UNIQUE (email),
    CONSTRAINT uk_newsletter_subscribers_token UNIQUE (token)
);
