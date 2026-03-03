-- =============================================================================
-- V17__agent_communication.sql – Agent Memory, Messaging, Sub-Agent Support
-- =============================================================================

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000001', false);

-- =============================================================================
-- 1. Agent Memories table
-- =============================================================================

CREATE TABLE agent_memories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    instance_id         UUID NOT NULL REFERENCES agent_instances(id),
    type                VARCHAR(30) NOT NULL DEFAULT 'NOTE',
    category            VARCHAR(100) NOT NULL,
    key                 VARCHAR(255) NOT NULL,
    value               TEXT NOT NULL,
    importance          INTEGER NOT NULL DEFAULT 5,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_accessed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_agent_memories_instance_key ON agent_memories(instance_id, key);
CREATE INDEX idx_agent_memories_tenant ON agent_memories(tenant_id);
CREATE INDEX idx_agent_memories_instance ON agent_memories(instance_id);
CREATE INDEX idx_agent_memories_expires ON agent_memories(expires_at) WHERE expires_at IS NOT NULL;

ALTER TABLE agent_memories ENABLE ROW LEVEL SECURITY;
CREATE POLICY agent_memories_tenant_isolation ON agent_memories
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- =============================================================================
-- 2. Agent Messages table
-- =============================================================================

CREATE TABLE agent_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    sender_instance_id  UUID NOT NULL REFERENCES agent_instances(id),
    target_instance_id  UUID NOT NULL REFERENCES agent_instances(id),
    message_type        VARCHAR(30) NOT NULL DEFAULT 'INFO',
    subject             VARCHAR(255) NOT NULL,
    body                TEXT NOT NULL,
    priority            VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parent_message_id   UUID REFERENCES agent_messages(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at        TIMESTAMPTZ,
    read_at             TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ
);

CREATE INDEX idx_agent_messages_tenant ON agent_messages(tenant_id);
CREATE INDEX idx_agent_messages_target ON agent_messages(target_instance_id, status);
CREATE INDEX idx_agent_messages_sender ON agent_messages(sender_instance_id);
CREATE INDEX idx_agent_messages_expires ON agent_messages(expires_at) WHERE expires_at IS NOT NULL;

ALTER TABLE agent_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY agent_messages_tenant_isolation ON agent_messages
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- =============================================================================
-- 3. Agent Instances: add spawned_by_run_id column
-- =============================================================================

ALTER TABLE agent_instances ADD COLUMN spawned_by_run_id UUID;

-- =============================================================================
-- 4. CEO Template: add memory + message tools
-- =============================================================================

UPDATE agent_templates
SET allowed_tools = '["delegate_to_lead","get_kpi_summary","check_messages","save_memory","recall_memory","read_agent_memory"]'
WHERE role = 'ceo';

-- =============================================================================
-- 5. Lead Templates: add memory, messaging, sub-agent tools
-- =============================================================================

UPDATE agent_templates SET allowed_tools =
'["get_jobs","get_job_detail","update_job_status","get_stations","get_production_kpis","send_message","report_to_ceo","check_messages","save_memory","recall_memory","spawn_sub_agent"]'
WHERE role = 'production_lead';

UPDATE agent_templates SET allowed_tools =
'["get_machines","get_machine_detail","get_maintenance_due","report_incident","schedule_maintenance","send_message","report_to_ceo","check_messages","save_memory","recall_memory","spawn_sub_agent"]'
WHERE role = 'machine_lead';

UPDATE agent_templates SET allowed_tools =
'["get_critical_stock","get_stock_level","get_stock_movements","create_reorder","send_message","report_to_ceo","check_messages","save_memory","recall_memory","spawn_sub_agent"]'
WHERE role = 'supply_lead';

UPDATE agent_templates SET allowed_tools =
'["get_attendance_today","get_absences","get_employee_detail","approve_absence","send_message","report_to_ceo","check_messages","save_memory","recall_memory","spawn_sub_agent"]'
WHERE role = 'people_lead';

UPDATE agent_templates SET allowed_tools =
'["get_customer_orders","get_open_conversations","get_conversation_detail","get_inbox_messages","create_inbox_reply","send_message","report_to_ceo","check_messages","save_memory","recall_memory","spawn_sub_agent"]'
WHERE role = 'support_lead';
