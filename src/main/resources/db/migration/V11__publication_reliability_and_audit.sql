ALTER TABLE background_job
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS heartbeat_at TIMESTAMP;

UPDATE background_job
SET heartbeat_at = COALESCE(started_at, updated_at, created_at)
WHERE heartbeat_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_background_job_status_priority_execute
    ON background_job(status, priority DESC, execute_after ASC);

ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP;

UPDATE outbox_event
SET next_attempt_at = COALESCE(next_attempt_at, created_at)
WHERE next_attempt_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_due
    ON outbox_event(status, next_attempt_at, created_at);

CREATE TABLE IF NOT EXISTS publication_audit (
    audit_id VARCHAR(36) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    version_id BIGINT,
    action VARCHAR(120) NOT NULL,
    actor VARCHAR(160) NOT NULL,
    correlation_id VARCHAR(160),
    before_json TEXT,
    after_json TEXT,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_publication_audit_owner FOREIGN KEY (owner_id) REFERENCES app_owner(owner_id) ON DELETE CASCADE,
    CONSTRAINT fk_publication_audit_version FOREIGN KEY (version_id) REFERENCES website_version(website_version_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_publication_audit_owner_created
    ON publication_audit(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_publication_audit_version_created
    ON publication_audit(version_id, created_at DESC);
