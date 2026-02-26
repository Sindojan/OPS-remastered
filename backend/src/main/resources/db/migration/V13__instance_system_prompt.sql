-- =============================================================================
-- V13__instance_system_prompt.sql – Add custom_system_prompt to agent_instances
-- Allows per-instance system prompt override (falls back to template base_prompt)
-- =============================================================================

ALTER TABLE agent_instances ADD COLUMN custom_system_prompt TEXT;
