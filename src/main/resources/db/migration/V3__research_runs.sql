CREATE TABLE research_runs (
    id VARCHAR(43) PRIMARY KEY,
    plan_id VARCHAR(43) NOT NULL REFERENCES plans(id),
    plan_version BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    researched_at TIMESTAMP WITH TIME ZONE,
    model VARCHAR(100) NOT NULL,
    result_json TEXT,
    error_message VARCHAR(400)
);
CREATE INDEX research_plan_started ON research_runs(plan_id, started_at);
CREATE INDEX research_started ON research_runs(started_at);
