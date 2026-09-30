-- One personal organization per existing user, then attach their projects.
INSERT INTO organizations (name, slug, created_at, updated_at)
SELECT u.name || '''s workspace',
       'user-' || u.id,
       NOW(),
       NOW()
FROM users u;

INSERT INTO organization_members (organization_id, user_id, role, created_at)
SELECT o.id, u.id, 'OWNER', NOW()
FROM users u
         JOIN organizations o ON o.slug = 'user-' || u.id;

ALTER TABLE projects
    ADD COLUMN organization_id BIGINT;

UPDATE projects p
SET organization_id = o.id
FROM organizations o
WHERE o.slug = 'user-' || p.user_id;

ALTER TABLE projects
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id);

CREATE INDEX idx_projects_organization_id ON projects (organization_id);
