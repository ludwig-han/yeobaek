CREATE TABLE plans (
    id VARCHAR(43) PRIMARY KEY,
    edit_key_hash VARCHAR(64) NOT NULL,
    title VARCHAR(100) NOT NULL,
    plan_date DATE NOT NULL,
    region VARCHAR(100) NOT NULL,
    anchor1 VARCHAR(300) NOT NULL,
    anchor2 VARCHAR(300) NOT NULL,
    meeting VARCHAR(1000) NOT NULL,
    transport VARCHAR(3000) NOT NULL,
    priorities VARCHAR(3000) NOT NULL,
    guardrails VARCHAR(3000) NOT NULL,
    backup VARCHAR(3000) NOT NULL,
    flexible VARCHAR(3000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
