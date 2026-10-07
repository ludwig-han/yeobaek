-- Preserve V1 notes and add structured user decisions.
ALTER TABLE plans ADD COLUMN meeting_style VARCHAR(30) DEFAULT 'UNKNOWN';
ALTER TABLE plans ADD COLUMN short_travel BOOLEAN DEFAULT FALSE;
ALTER TABLE plans ADD COLUMN few_transfers BOOLEAN DEFAULT FALSE;
ALTER TABLE plans ADD COLUMN avoid_crowds BOOLEAN DEFAULT FALSE;
ALTER TABLE plans ADD COLUMN low_cost BOOLEAN DEFAULT FALSE;
ALTER TABLE plans ADD COLUMN fair_travel BOOLEAN DEFAULT FALSE;
ALTER TABLE plans ADD COLUMN walking VARCHAR(30) DEFAULT 'UNKNOWN';
ALTER TABLE plans ADD COLUMN max_wait_minutes INTEGER DEFAULT NULL;
ALTER TABLE plans ADD COLUMN avoid_late_return VARCHAR(30) DEFAULT 'UNKNOWN';
ALTER TABLE plans ADD COLUMN return_by TIME DEFAULT NULL;
ALTER TABLE plans ADD COLUMN budget_per_person INTEGER DEFAULT NULL;
ALTER TABLE plans ADD COLUMN weather VARCHAR(30) DEFAULT 'UNKNOWN';
CREATE TABLE plan_anchors (
    plan_id VARCHAR(43) NOT NULL REFERENCES plans(id),
    position INTEGER NOT NULL,
    goal_name VARCHAR(300) DEFAULT '',
    kind VARCHAR(30) DEFAULT 'UNKNOWN',
    place_name VARCHAR(150) DEFAULT '',
    place_rule VARCHAR(30) DEFAULT 'UNKNOWN',
    time_sensitive VARCHAR(30) DEFAULT 'UNKNOWN',
    earliest TIME DEFAULT NULL,
    latest TIME DEFAULT NULL,
    stay_important VARCHAR(30) DEFAULT 'UNKNOWN',
    stay_minutes INTEGER DEFAULT NULL,
    backup_needed VARCHAR(30) DEFAULT 'UNKNOWN',
    note VARCHAR(300) DEFAULT '',
    PRIMARY KEY (plan_id, position)
);
CREATE TABLE plan_candidates (
    plan_id VARCHAR(43) NOT NULL REFERENCES plans(id),
    position INTEGER NOT NULL,
    candidate_name VARCHAR(150) DEFAULT '',
    kind VARCHAR(30) DEFAULT 'UNKNOWN',
    importance VARCHAR(30) DEFAULT 'NORMAL',
    note VARCHAR(300) DEFAULT '',
    PRIMARY KEY (plan_id, position)
);
CREATE TABLE plan_participants (
    plan_id VARCHAR(43) NOT NULL REFERENCES plans(id),
    position INTEGER NOT NULL,
    participant_label VARCHAR(50) DEFAULT '',
    origin VARCHAR(150) DEFAULT '',
    PRIMARY KEY (plan_id, position)
);
INSERT INTO plan_anchors(plan_id,position,goal_name) SELECT id,0,anchor1 FROM plans;
INSERT INTO plan_anchors(plan_id,position,goal_name) SELECT id,1,anchor2 FROM plans WHERE anchor2 <> '';
