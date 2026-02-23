-- V6: Seed Agent Instances (CEO + 7 Lead Agents, all PERSISTENT)

-- CEO Agent Instance (no parent)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'ceo' LIMIT 1),
    'CEO Agent',
    NULL,
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-sonnet-4-20250514"}',
    NOW(), NOW()
);

-- Production Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'production_lead' LIMIT 1),
    'Production Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Support Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'support_lead' LIMIT 1),
    'Support Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Supply Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'supply_lead' LIMIT 1),
    'Supply Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- People Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'people_lead' LIMIT 1),
    'People Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Machine Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'machine_lead' LIMIT 1),
    'Machine Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Knowledge Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'knowledge_lead' LIMIT 1),
    'Knowledge Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Finance Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, template_id, name, parent_instance_id, type, status, tenant_id, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM agent_templates WHERE role = 'finance_lead' LIMIT 1),
    'Finance Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    NULL,
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);
