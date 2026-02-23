-- =============================================================================
-- V1__init_public.sql – Core tables: tenants, users, refresh_token_blacklist
-- Single-schema with RLS (Row Level Security)
-- =============================================================================

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'WORKER',
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, email)
);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_token_blacklist (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expired_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_blacklist_expired_at ON refresh_token_blacklist(expired_at);

-- Default Tenant
INSERT INTO tenants (id, name, active) VALUES ('00000000-0000-0000-0000-000000000001', 'Owlsburg Manufaktur', true);

-- Default Admin
INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'software@sindojan.de', '$2a$10$1i2Bw8xl65MuiVYtKVX53O.oETZKLJGnVNmpCDVXN.Up0aJThI9Z6', 'Admin', 'User', 'ADMIN', true);
