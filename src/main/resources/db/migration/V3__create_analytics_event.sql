CREATE TABLE IF NOT EXISTS analytics_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    page_path TEXT,
    page_title VARCHAR(300),
    project_slug VARCHAR(255),
    referrer TEXT,
    source VARCHAR(255),
    medium VARCHAR(255),
    campaign VARCHAR(255),
    recruiter_code VARCHAR(255),
    visitor_id_hash VARCHAR(128),
    session_id_hash VARCHAR(128),
    device_type VARCHAR(50),
    browser VARCHAR(120),
    os VARCHAR(120),
    language VARCHAR(50),
    screen_width INTEGER,
    screen_height INTEGER,
    user_agent VARCHAR(600),
    country VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analytics_event_created_at ON analytics_event (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_event_event_type ON analytics_event (event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_event_visitor ON analytics_event (visitor_id_hash);
CREATE INDEX IF NOT EXISTS idx_analytics_event_session ON analytics_event (session_id_hash);
CREATE INDEX IF NOT EXISTS idx_analytics_event_project_slug ON analytics_event (project_slug);
CREATE INDEX IF NOT EXISTS idx_analytics_event_recruiter_code ON analytics_event (recruiter_code);
