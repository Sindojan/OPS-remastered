-- V16: LLM Model Normalization
-- Normalize model strings in agent_instances config JSONB
-- CEO → claude-opus-4-6, Leads → claude-sonnet-4-6

-- CEO instance: set model to opus
UPDATE agent_instances
SET config = jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{model}',
    '"claude-opus-4-6"'
)
WHERE name LIKE 'CEO%';

-- Lead instances: set model to sonnet
UPDATE agent_instances
SET config = jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{model}',
    '"claude-sonnet-4-6"'
)
WHERE name IN ('Production Lead', 'Machine Lead', 'Supply Lead', 'People Lead', 'Support Lead');

-- Normalize any remaining instances with invalid/old model strings to sonnet
UPDATE agent_instances
SET config = jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{model}',
    '"claude-sonnet-4-6"'
)
WHERE config->>'model' IS NULL
   OR config->>'model' NOT IN ('claude-opus-4-6', 'claude-sonnet-4-6', 'claude-haiku-4-5-20251001');
