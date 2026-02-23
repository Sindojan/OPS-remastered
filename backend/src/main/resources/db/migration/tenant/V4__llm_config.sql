-- V4: LLM Provider Configuration per Tenant
CREATE TABLE tenant_llm_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL DEFAULT 'anthropic',
    api_key_enc TEXT,
    default_model VARCHAR(100) DEFAULT 'claude-sonnet-4-20250514',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
