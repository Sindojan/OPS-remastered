-- V21: Agent & LLM Configuration Enhancements
-- Erweiterbare LLM-Settings (temperature, maxTokensDefault, etc.)
ALTER TABLE tenant_llm_config ADD COLUMN settings JSONB DEFAULT '{}';

-- Instance-Level Tool-Override (NULL = Template-Defaults)
ALTER TABLE agent_instances ADD COLUMN allowed_tools_override JSONB DEFAULT NULL;
