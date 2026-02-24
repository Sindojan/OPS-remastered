-- =============================================================================
-- V5__primary_agent.sql – Role-to-Agent defaults & user primary agent
-- =============================================================================

-- Role-to-Agent Default Mapping (per tenant, configurable)
CREATE TABLE role_agent_defaults (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    role                VARCHAR(50) NOT NULL,
    agent_instance_id   UUID NOT NULL REFERENCES agent_instances(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, role)
);

CREATE INDEX idx_role_agent_defaults_tenant ON role_agent_defaults(tenant_id);

-- RLS
ALTER TABLE role_agent_defaults ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_agent_defaults FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON role_agent_defaults
    USING (tenant_id = current_tenant_id());

-- User-specific override
ALTER TABLE users
    ADD COLUMN primary_agent_instance_id UUID REFERENCES agent_instances(id);
