ALTER TABLE audit_pages
    ADD COLUMN final_url      VARCHAR(2048),
    ADD COLUMN redirect_chain JSONB,
    ADD COLUMN skip_reason    VARCHAR(500);
