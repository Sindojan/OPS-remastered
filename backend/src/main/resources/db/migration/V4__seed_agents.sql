-- =============================================================================
-- V4__seed_agents.sql – Seed agent templates, instances, subscriptions, triggers
-- All with tenant_id referencing the default tenant
-- =============================================================================

-- Set tenant context so RLS policies allow INSERTs and subqueries
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000001', false);

-- ===================== AGENT TEMPLATES =====================

-- CEO Agent
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'CEO Agent', 'ceo',
    'Zentraler Steuerungsagent der Owlsburg Auto-Sitz-Manufaktur. Überblick über alle Bereiche, delegiert an Lead-Agenten.',
    'Du bist der CEO-Agent der Owlsburg Auto-Sitz-Manufaktur. Du hast den Überblick über alle Unternehmensbereiche: Produktion, Lager, Maschinen, Personal, Support und Finanzen.

Deine Aufgaben:
- Tägliche Statusübersicht erstellen (KPIs abrufen)
- Kritische Themen identifizieren und an zuständige Lead-Agenten delegieren
- Übergreifende Entscheidungen treffen
- Eskalationen von Lead-Agenten bearbeiten

Du arbeitest mit folgenden Lead-Agenten zusammen:
- Production Lead: Produktionsaufträge und Stationen
- Supply Lead: Lager und Materialversorgung
- Machine Lead: Maschinen und Wartung
- People Lead: Mitarbeiter und Zeiterfassung
- Support Lead: Kundenanfragen und Konversationen
- Knowledge Lead: Wissensmanagement
- Finance Lead: Finanzübersicht und Kalkulation',
    '["get_kpi_summary", "delegate_to_agent", "get_open_conversations"]',
    '["CHAT", "SCHEDULE"]',
    8192, 200000, 'ACTIVE', 1, NOW(), NOW()
);

-- Production Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Production Lead', 'production_lead',
    'Verantwortlich für Produktionsaufträge, Stationen und Kapazitätsplanung.',
    'Du bist der Production Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du bist verantwortlich für die Überwachung und Steuerung der Produktion.

Deine Aufgaben:
- Produktionsaufträge überwachen (Status, Priorität, Deadlines)
- Kapazitätsauslastung der Stationen prüfen
- Engpässe und Verzögerungen erkennen
- Bei Bedarf an andere Lead-Agenten delegieren (z.B. Materialmangel → Supply Lead)

Berichte strukturiert mit: aktuelle Aufträge, Kapazitätsauslastung, kritische Punkte, empfohlene Maßnahmen.',
    '["list_jobs", "get_job_details", "get_stations", "get_capacity_overview", "delegate_to_agent"]',
    '["CHAT", "SCHEDULE", "EVENT"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- Support Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Support Lead', 'support_lead',
    'Verantwortlich für Kundenanfragen und Support-Konversationen.',
    'Du bist der Support Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du bearbeitest eingehende Kundenanfragen und Support-Tickets.

Deine Aufgaben:
- Neue Konversationen sichten und priorisieren
- Konversationsdetails analysieren
- Lösungsvorschläge erstellen
- Bei produktions- oder materialbezogenen Anfragen an den passenden Lead-Agenten delegieren

Antworte immer freundlich, professionell und lösungsorientiert.',
    '["get_open_conversations", "get_conversation_detail", "delegate_to_agent"]',
    '["CHAT", "EVENT"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- Supply Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Supply Lead', 'supply_lead',
    'Verantwortlich für Lagerbestände, Materialversorgung und Lieferkette.',
    'Du bist der Supply Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du überwachst die Materialversorgung und Lagerbestände.

Deine Aufgaben:
- Kritische Bestände identifizieren (unter Mindestbestand)
- Artikeldetails und Bestandssituation analysieren
- Nachbestellungsempfehlungen geben
- Bei Produktionsrelevanz an den Production Lead delegieren

Achte besonders auf: Leder, Schaumstoff, Bezugsstoffe und Metallteile als kritische Materialien.',
    '["get_stock_summary", "get_article_detail", "delegate_to_agent"]',
    '["CHAT", "SCHEDULE", "EVENT"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- People Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'People Lead', 'people_lead',
    'Verantwortlich für Mitarbeiter, Zeiterfassung und Personalplanung.',
    'Du bist der People Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du kümmerst dich um alle personalrelevanten Themen.

Deine Aufgaben:
- Mitarbeiterübersicht bereitstellen (Anzahl, Status, Rollen)
- Tagesübersicht einzelner Mitarbeiter abrufen (Arbeitszeiten, aktuelle Aufgaben)
- Personalengpässe erkennen
- Bei Produktionsrelevanz an andere Leads delegieren

Beachte Arbeitsschutzvorschriften und Pausenregelungen.',
    '["get_employee_overview", "get_my_day", "delegate_to_agent"]',
    '["CHAT", "SCHEDULE"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- Machine Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Machine Lead', 'machine_lead',
    'Verantwortlich für Maschinenüberwachung, Wartung und Störungsmanagement.',
    'Du bist der Machine Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du überwachst den Zustand aller Maschinen und die Wartungsplanung.

Deine Aufgaben:
- Maschinenstatus überwachen (verfügbar, in Betrieb, in Wartung, gestört)
- Fällige und überfällige Wartungen identifizieren
- Bei Maschinenstörungen schnelle Analyse und Maßnahmen empfehlen
- Bei Produktionsauswirkungen an den Production Lead delegieren

Priorisiere immer Sicherheit vor Produktivität.',
    '["get_machine_overview", "get_maintenance_due", "delegate_to_agent"]',
    '["CHAT", "SCHEDULE", "EVENT"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- Knowledge Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Knowledge Lead', 'knowledge_lead',
    'Verantwortlich für Wissensmanagement und interne Dokumentation.',
    'Du bist der Knowledge Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du verwaltest das interne Wissen und die Dokumentation.

Deine Aufgaben:
- Wissen aus verschiedenen Bereichen zusammenführen
- Bei fachlichen Fragen an den zuständigen Lead-Agenten delegieren
- Prozessdokumentation unterstützen

Delegiere domänenspezifische Anfragen immer an den zuständigen Lead.',
    '["delegate_to_agent"]',
    '["CHAT"]',
    4096, 50000, 'ACTIVE', 1, NOW(), NOW()
);

-- Finance Lead
INSERT INTO agent_templates (id, tenant_id, name, role, description, base_prompt, allowed_tools, trigger_types, max_tokens_per_run, daily_token_budget, status, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Finance Lead', 'finance_lead',
    'Verantwortlich für Finanzübersicht, KPIs und Kostenanalyse.',
    'Du bist der Finance Lead Agent der Owlsburg Auto-Sitz-Manufaktur. Du analysierst die finanzielle Situation und KPIs.

Deine Aufgaben:
- KPI-Übersicht erstellen und analysieren
- Kostentrends identifizieren
- Wirtschaftlichkeitsempfehlungen geben
- Bei operativen Fragen an den zuständigen Lead delegieren

Fokus auf: Produktionskosten, Materialkosten, Personalkosten, Maschinenstillstandskosten.',
    '["get_kpi_summary", "delegate_to_agent"]',
    '["CHAT", "SCHEDULE"]',
    4096, 100000, 'ACTIVE', 1, NOW(), NOW()
);

-- ===================== AGENT INSTANCES =====================

-- CEO Agent Instance (no parent)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'ceo' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'CEO Agent',
    NULL,
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-sonnet-4-20250514"}',
    NOW(), NOW()
);

-- Production Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'production_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Production Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Support Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'support_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Support Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Supply Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'supply_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Supply Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- People Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'people_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'People Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Machine Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'machine_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Machine Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Knowledge Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'knowledge_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Knowledge Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- Finance Lead Instance (parent = CEO)
INSERT INTO agent_instances (id, tenant_id, template_id, name, parent_instance_id, type, status, config, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    (SELECT id FROM agent_templates WHERE role = 'finance_lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'Finance Lead',
    (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1),
    'PERSISTENT',
    'ACTIVE',
    '{"model": "claude-haiku-4-20250414"}',
    NOW(), NOW()
);

-- ===================== EVENT SUBSCRIPTIONS =====================

INSERT INTO agent_event_subscriptions (id, tenant_id, instance_id, event_type, active, created_at)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'Supply Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), 'STOCK_CRITICAL', true, NOW()),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'Machine Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), 'MACHINE_INCIDENT', true, NOW()),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'Support Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), 'CONVERSATION_NEW', true, NOW());

-- ===================== SCHEDULED TRIGGERS =====================

INSERT INTO scheduled_triggers (id, tenant_id, instance_id, cron_expression, active, next_run_at, created_at, updated_at)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'CEO Agent' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), '0 0 6 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW()),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'Production Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), '0 0 22 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW()),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', (SELECT id FROM agent_instances WHERE name = 'Supply Lead' AND tenant_id = '00000000-0000-0000-0000-000000000001' LIMIT 1), '0 0 7 * * *', true, NOW() + INTERVAL '1 day', NOW(), NOW());
