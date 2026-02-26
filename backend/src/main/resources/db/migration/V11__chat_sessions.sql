CREATE TABLE chat_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES public.tenants(id),
    user_id             UUID NOT NULL REFERENCES public.users(id),
    agent_instance_id   UUID NOT NULL REFERENCES agent_instances(id),
    title               VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES public.tenants(id),
    session_id  UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_sessions_user ON chat_sessions(tenant_id, user_id);
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at);

-- RLS
ALTER TABLE chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON chat_sessions USING (tenant_id = current_tenant_id());

ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON chat_messages USING (tenant_id = current_tenant_id());
