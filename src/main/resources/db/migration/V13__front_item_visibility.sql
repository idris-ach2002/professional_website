CREATE TABLE front_item_visibility (
    item_key VARCHAR(180) PRIMARY KEY,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_front_item_visibility_updated_at
    ON front_item_visibility (updated_at DESC);
