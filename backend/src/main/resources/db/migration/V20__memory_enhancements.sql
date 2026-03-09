-- V20: Memory System Enhancements
-- Adds source tracking, metadata JSONB, and performance indexes

ALTER TABLE agent_memories ADD COLUMN source VARCHAR(50) DEFAULT 'agent';
ALTER TABLE agent_memories ADD COLUMN metadata JSONB DEFAULT '{}';

CREATE INDEX idx_agent_memories_type ON agent_memories(instance_id, type);
CREATE INDEX idx_agent_memories_source ON agent_memories(instance_id, source);
