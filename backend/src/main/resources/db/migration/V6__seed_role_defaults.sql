-- =============================================================================
-- V6__seed_role_defaults.sql – Seed role-to-agent default mappings
-- =============================================================================

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000001', false);

-- ADMIN + MANAGER -> CEO Agent
-- TEAM_LEAD -> Production Lead
-- WORKER -> People Lead
INSERT INTO role_agent_defaults (tenant_id, role, agent_instance_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN',
     (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1)),
    ('00000000-0000-0000-0000-000000000001', 'MANAGER',
     (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1)),
    ('00000000-0000-0000-0000-000000000001', 'TEAM_LEAD',
     (SELECT id FROM agent_instances WHERE name = 'Production Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1)),
    ('00000000-0000-0000-0000-000000000001', 'WORKER',
     (SELECT id FROM agent_instances WHERE name = 'People Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1));
