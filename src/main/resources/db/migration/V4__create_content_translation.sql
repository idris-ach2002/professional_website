CREATE SEQUENCE IF NOT EXISTS content_translation_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS content_translation (
    translation_id BIGINT PRIMARY KEY DEFAULT nextval('content_translation_seq'),
    content_type VARCHAR(40) NOT NULL,
    content_key VARCHAR(160) NOT NULL,
    locale VARCHAR(12) NOT NULL,
    field_name VARCHAR(80) NOT NULL,
    translated_text TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_content_translation UNIQUE (content_type, content_key, locale, field_name),
    CONSTRAINT ck_content_translation_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_content_translation_locale CHECK (locale <> '')
);

CREATE INDEX IF NOT EXISTS idx_content_translation_lookup
    ON content_translation (content_type, content_key, locale, status);

CREATE INDEX IF NOT EXISTS idx_content_translation_locale
    ON content_translation (locale, status);
