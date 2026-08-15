CREATE TABLE runtime_performance_sample (
    id UUID PRIMARY KEY,
    build_id VARCHAR(120) NOT NULL,
    runtime_profile VARCHAR(24) NOT NULL,
    memory_state VARCHAR(24) NOT NULL,
    fps DOUBLE PRECISION,
    frame_p95_ms DOUBLE PRECISION,
    long_task_count INTEGER NOT NULL DEFAULT 0,
    worker_latency_ms DOUBLE PRECISION,
    api_latency_ms DOUBLE PRECISION,
    active_resources INTEGER NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_runtime_performance_sample_recorded_at
    ON runtime_performance_sample (recorded_at DESC);

CREATE INDEX idx_runtime_performance_sample_build
    ON runtime_performance_sample (build_id, recorded_at DESC);
