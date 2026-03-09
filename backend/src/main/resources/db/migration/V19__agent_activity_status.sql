-- V19: Agent Activity Status (IDLE/BUSY/ERROR) + Last Run Reference
ALTER TABLE agent_instances ADD COLUMN activity_status VARCHAR(10) NOT NULL DEFAULT 'IDLE';
ALTER TABLE agent_instances ADD COLUMN last_run_id UUID REFERENCES agent_runs(id);
ALTER TABLE agent_instances ADD COLUMN activity_status_changed_at TIMESTAMPTZ;

CREATE INDEX idx_agent_instances_activity_status ON agent_instances(activity_status);
