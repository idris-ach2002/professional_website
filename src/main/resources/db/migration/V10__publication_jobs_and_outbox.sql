ALTER TABLE website_version
    ADD COLUMN IF NOT EXISTS publication_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS publication_error VARCHAR(2000);

UPDATE website_version
SET publication_status = CASE
    WHEN active = TRUE AND published = TRUE THEN 'PUBLISHED'
    WHEN published = TRUE THEN 'READY'
    ELSE 'DRAFT'
END
WHERE publication_status = 'DRAFT';

CREATE TABLE IF NOT EXISTS background_job (
    job_id VARCHAR(36) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    version_id BIGINT,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    execute_after TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    last_error VARCHAR(2000),
    correlation_id VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_background_job_owner FOREIGN KEY (owner_id) REFERENCES app_owner(owner_id) ON DELETE CASCADE,
    CONSTRAINT fk_background_job_version FOREIGN KEY (version_id) REFERENCES website_version(website_version_id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_background_job_owner_created ON background_job(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_background_job_status_execute ON background_job(status, execute_after);

CREATE TABLE IF NOT EXISTS outbox_event (
    event_id VARCHAR(36) PRIMARY KEY,
    event_key VARCHAR(180) NOT NULL,
    owner_id BIGINT NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    dispatched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_outbox_event_key UNIQUE(event_key),
    CONSTRAINT fk_outbox_owner FOREIGN KEY (owner_id) REFERENCES app_owner(owner_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_event(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_owner_created ON outbox_event(owner_id, created_at DESC);
