-- V22 forward-only bootstrap migration.
-- Existing installations already at V3-V5 can apply this safely; fresh databases
-- run V3-V5 first and then receive the complete portfolio core schema here.
-- V22 bootstrap baseline: a fresh PostgreSQL database must be reproducible
-- exclusively from Flyway while Hibernate remains in ddl-auto=validate mode.

CREATE SEQUENCE IF NOT EXISTS owner_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS website_version_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS profile_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS timeline_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS experience_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS project_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS app_owner (
    owner_id BIGINT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    first_name VARCHAR(256) NOT NULL,
    age INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    address VARCHAR(256) NOT NULL
);

CREATE TABLE IF NOT EXISTS profile (
    profile_id BIGINT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(180),
    headline VARCHAR(256),
    short_description VARCHAR(500),
    description TEXT NOT NULL,
    location VARCHAR(120),
    availability VARCHAR(160),
    profile_image_url VARCHAR(512),
    logo_url VARCHAR(512),
    cv_url VARCHAR(512),
    portfolio_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS timeline (
    timeline_id BIGINT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(300)
);

CREATE TABLE IF NOT EXISTS website_version (
    website_version_id BIGINT PRIMARY KEY,
    version_tag VARCHAR(80) NOT NULL,
    label VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    owner_id BIGINT NOT NULL,
    profile_id BIGINT,
    timeline_id BIGINT,
    CONSTRAINT fk_website_version_owner FOREIGN KEY (owner_id) REFERENCES app_owner(owner_id) ON DELETE CASCADE,
    CONSTRAINT fk_website_version_profile FOREIGN KEY (profile_id) REFERENCES profile(profile_id),
    CONSTRAINT fk_website_version_timeline FOREIGN KEY (timeline_id) REFERENCES timeline(timeline_id),
    CONSTRAINT uk_owner_version_tag UNIQUE (owner_id, version_tag),
    CONSTRAINT uk_website_version_profile UNIQUE (profile_id),
    CONSTRAINT uk_website_version_timeline UNIQUE (timeline_id)
);

CREATE TABLE IF NOT EXISTS owner_contacts (
    owner_id BIGINT NOT NULL,
    contact_type VARCHAR(50) NOT NULL,
    contact_value VARCHAR(512) NOT NULL,
    CONSTRAINT fk_owner_contacts_owner FOREIGN KEY (owner_id) REFERENCES app_owner(owner_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experience (
    experience_id BIGINT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    organization VARCHAR(160),
    location VARCHAR(160),
    summary VARCHAR(500),
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    current_position BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(512),
    website_url VARCHAR(512),
    display_order INTEGER,
    timeline_id BIGINT,
    CONSTRAINT fk_experience_timeline FOREIGN KEY (timeline_id) REFERENCES timeline(timeline_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experience_skills (
    experience_id BIGINT NOT NULL,
    skill VARCHAR(100) NOT NULL,
    CONSTRAINT fk_experience_skills_experience FOREIGN KEY (experience_id) REFERENCES experience(experience_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project (
    project_id BIGINT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    subtitle VARCHAR(300),
    short_description VARCHAR(500),
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date DATE,
    image_url VARCHAR(512),
    demo_url VARCHAR(512),
    github_url VARCHAR(512),
    documentation_url VARCHAR(512),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER,
    website_version_id BIGINT NOT NULL,
    CONSTRAINT fk_project_website_version FOREIGN KEY (website_version_id) REFERENCES website_version(website_version_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project_stacks (
    project_id BIGINT NOT NULL,
    stack VARCHAR(100) NOT NULL,
    CONSTRAINT fk_project_stacks_project FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project_features (
    project_id BIGINT NOT NULL,
    feature VARCHAR(255) NOT NULL,
    CONSTRAINT fk_project_features_project FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project_links (
    project_id BIGINT NOT NULL,
    link_type VARCHAR(50) NOT NULL,
    label VARCHAR(100),
    url VARCHAR(512) NOT NULL,
    CONSTRAINT fk_project_links_project FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE
);
-- Enforce one active version per owner at the database level as a final
-- concurrency safety net. PostgreSQL partial indexes are ideal for this rule.
CREATE UNIQUE INDEX IF NOT EXISTS uk_website_version_one_active_per_owner
    ON website_version(owner_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_website_version_owner_created
    ON website_version(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_website_version_public
    ON website_version(owner_id, active, published);
CREATE INDEX IF NOT EXISTS idx_project_version_order
    ON project(website_version_id, display_order, start_date DESC);
CREATE INDEX IF NOT EXISTS idx_project_public
    ON project(website_version_id, published, display_order);
CREATE INDEX IF NOT EXISTS idx_experience_timeline_date
    ON experience(timeline_id, start_date DESC);
