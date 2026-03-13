-- V22: Performance indexes for growing agent tables

-- Budget-Check: findByInstanceIdAndStartedAtBetween
CREATE INDEX idx_agent_runs_instance_started ON agent_runs(instance_id, started_at);

-- Step-Ordering: findByRunIdOrderByStepNumber
CREATE INDEX idx_agent_run_steps_run_step ON agent_run_steps(run_id, step_number);

-- Memory LRU-Eviction
CREATE INDEX idx_agent_memories_lru ON agent_memories(instance_id, updated_at);

-- Message Inbox Queries
CREATE INDEX idx_agent_messages_inbox ON agent_messages(target_instance_id, status, created_at);

-- Tenant-Aggregation (System Admin Stats)
CREATE INDEX idx_agent_runs_status_started ON agent_runs(status, started_at);

-- Retention: abgeschlossene Runs nach Datum finden
CREATE INDEX idx_agent_runs_completed ON agent_runs(completed_at) WHERE completed_at IS NOT NULL;

-- Retention: Chat-Messages nach Alter
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at);
