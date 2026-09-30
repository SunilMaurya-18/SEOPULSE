CREATE TABLE organizations
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(150)             NOT NULL,
    slug               VARCHAR(80)              NOT NULL,
    stripe_customer_id VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_organizations_slug UNIQUE (slug)
);

CREATE TABLE organization_members
(
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT                   NOT NULL,
    user_id         BIGINT                   NOT NULL,
    role            VARCHAR(20)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_org_members_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_org_members_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_org_members UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_user ON organization_members (user_id);

CREATE TABLE invitations
(
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT                   NOT NULL,
    email           VARCHAR(255)             NOT NULL,
    role            VARCHAR(20)              NOT NULL,
    token_hash      VARCHAR(64)              NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at     TIMESTAMP WITH TIME ZONE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_invitations_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uk_invitations_token UNIQUE (token_hash)
);

CREATE INDEX idx_invitations_org ON invitations (organization_id);

CREATE TABLE audit_log
(
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT,
    actor_user_id   BIGINT,
    action          VARCHAR(80)              NOT NULL,
    detail          VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_log_org ON audit_log (organization_id, created_at);
