ALTER TABLE website_version
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

-- Move the PostgreSQL-only invariant previously stored as a loose SQL file into
-- the versioned migration chain.
CREATE UNIQUE INDEX IF NOT EXISTS uk_website_version_one_active_per_owner
    ON website_version(owner_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_owner_active
    ON app_owner(active, owner_id);
CREATE INDEX IF NOT EXISTS idx_translation_public_lookup
    ON content_translation(locale, status, content_type, content_key);
CREATE INDEX IF NOT EXISTS idx_analytics_event_created_type
    ON analytics_event(created_at DESC, event_type);
