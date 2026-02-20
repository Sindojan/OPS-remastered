-- Public schema: tenant-übergreifende Stammdaten

CREATE TABLE IF NOT EXISTS tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(63) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    schema_name     VARCHAR(63) NOT NULL UNIQUE,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenants_tenant_id ON tenants(tenant_id);
CREATE INDEX idx_tenants_active ON tenants(active);
