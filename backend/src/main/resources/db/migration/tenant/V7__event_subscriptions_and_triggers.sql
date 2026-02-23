-- Agent Event Subscriptions table
CREATE TABLE agent_event_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL REFERENCES agent_instances(id),
    event_type VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_event_subs_instance ON agent_event_subscriptions(instance_id);
CREATE INDEX idx_agent_event_subs_event_type ON agent_event_subscriptions(event_type);

-- Seed subscriptions (use subqueries to find instance IDs by name)
INSERT INTO agent_event_subscriptions (id, instance_id, event_type, active, created_at)
VALUES
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'Supply Lead' LIMIT 1), 'STOCK_CRITICAL', true, NOW()),
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'Machine Lead' LIMIT 1), 'MACHINE_INCIDENT', true, NOW()),
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'Support Lead' LIMIT 1), 'CONVERSATION_NEW', true, NOW());

-- Seed scheduled triggers (use subqueries to find instance IDs by name)
INSERT INTO scheduled_triggers (id, instance_id, cron_expression, active, next_run_at, created_at, updated_at)
VALUES
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'CEO Agent' LIMIT 1), '0 0 6 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW()),
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'Production Lead' LIMIT 1), '0 0 22 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW()),
    (gen_random_uuid(), (SELECT id FROM agent_instances WHERE name = 'Supply Lead' LIMIT 1), '0 0 7 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW());
