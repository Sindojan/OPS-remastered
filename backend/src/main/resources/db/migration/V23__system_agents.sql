-- V23: System-Level Agent Infrastructure (kein tenant_id, kein RLS)
-- Parallel zum Tenant-Agent-System für Plattform-Verwaltung durch SYSTEM_ADMIN

-- ─── System Agent Templates ───
CREATE TABLE system_agent_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    role            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    base_prompt     TEXT,
    allowed_tools   JSONB NOT NULL DEFAULT '[]',
    trigger_types   JSONB NOT NULL DEFAULT '[]',
    max_tokens_per_run  INT NOT NULL DEFAULT 4096,
    daily_token_budget  INT NOT NULL DEFAULT 200000,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Instances ───
CREATE TABLE system_agent_instances (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id             UUID NOT NULL REFERENCES system_agent_templates(id),
    name                    VARCHAR(255) NOT NULL,
    parent_instance_id      UUID REFERENCES system_agent_instances(id),
    type                    VARCHAR(20) NOT NULL DEFAULT 'PERSISTENT',
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    config                  JSONB DEFAULT '{}',
    custom_system_prompt    TEXT,
    activity_status         VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    last_run_id             UUID,
    activity_status_changed_at TIMESTAMPTZ,
    allowed_tools_override  JSONB,
    terminated_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Runs ───
CREATE TABLE system_agent_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES system_agent_instances(id),
    trigger_type    VARCHAR(30) NOT NULL,
    trigger_source  VARCHAR(255),
    input_context   JSONB,
    output          JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    tokens_used     INT NOT NULL DEFAULT 0,
    cost_usd        NUMERIC(10,6) NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Run Steps ───
CREATE TABLE system_agent_run_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES system_agent_runs(id) ON DELETE CASCADE,
    step_number     INT NOT NULL,
    type            VARCHAR(30) NOT NULL,
    tool_name       VARCHAR(100),
    input           JSONB,
    output          JSONB,
    tokens_used     INT NOT NULL DEFAULT 0,
    duration_ms     INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Memories ───
CREATE TABLE system_agent_memories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES system_agent_instances(id),
    type            VARCHAR(30) NOT NULL DEFAULT 'NOTE',
    category        VARCHAR(100) NOT NULL,
    key             VARCHAR(255) NOT NULL,
    value           TEXT NOT NULL,
    importance      INT NOT NULL DEFAULT 5,
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ,
    source          VARCHAR(50) DEFAULT 'agent',
    metadata        JSONB DEFAULT '{}',
    run_id          UUID,
    confidence      NUMERIC(3,2) DEFAULT 1.0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Messages ───
CREATE TABLE system_agent_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_instance_id  UUID NOT NULL REFERENCES system_agent_instances(id),
    target_instance_id  UUID NOT NULL REFERENCES system_agent_instances(id),
    message_type        VARCHAR(30) NOT NULL DEFAULT 'INFO',
    subject             VARCHAR(255) NOT NULL,
    body                TEXT NOT NULL,
    priority            VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parent_message_id   UUID REFERENCES system_agent_messages(id),
    delivered_at        TIMESTAMPTZ,
    read_at             TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Agent Incidents ───
CREATE TABLE system_agent_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES system_agent_instances(id),
    type            VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ
);

-- ─── System Chat Sessions ───
CREATE TABLE system_chat_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    agent_instance_id   UUID NOT NULL REFERENCES system_agent_instances(id),
    title               VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Chat Messages ───
CREATE TABLE system_chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES system_chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System API Credentials ───
CREATE TABLE system_api_credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name    VARCHAR(100) NOT NULL UNIQUE,
    encrypted_value TEXT NOT NULL,
    description     VARCHAR(500),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System LLM Config ───
CREATE TABLE system_llm_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider        VARCHAR(50) NOT NULL DEFAULT 'ANTHROPIC',
    api_key_enc     TEXT NOT NULL,
    default_model   VARCHAR(100) NOT NULL DEFAULT 'claude-sonnet-4-6',
    settings        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── System Scheduled Triggers ───
CREATE TABLE system_scheduled_triggers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id         UUID NOT NULL REFERENCES system_agent_instances(id),
    cron_expression     VARCHAR(100) NOT NULL,
    task_description    TEXT NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    last_run_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── Performance Indexes ───
CREATE INDEX idx_sys_agent_runs_instance_started ON system_agent_runs(instance_id, started_at);
CREATE INDEX idx_sys_agent_run_steps_run_step ON system_agent_run_steps(run_id, step_number);
CREATE INDEX idx_sys_agent_memories_instance_cat ON system_agent_memories(instance_id, category);
CREATE INDEX idx_sys_agent_memories_lru ON system_agent_memories(instance_id, last_accessed_at);
CREATE INDEX idx_sys_agent_messages_inbox ON system_agent_messages(target_instance_id, status);
CREATE INDEX idx_sys_chat_messages_session ON system_chat_messages(session_id, created_at);
CREATE INDEX idx_sys_chat_sessions_user ON system_chat_sessions(user_id);

-- ═══════════════════════════════════════════
-- SEED: 9 System Agent Templates + Instances
-- ═══════════════════════════════════════════

-- System CEO Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000001'::uuid, 'System CEO', 'system_ceo',
 'Zentraler System-Agent für Plattformverwaltung. Koordiniert 8 Lead-Agents.',
 E'Du bist der System-CEO von Owlsburg OPS, der zentralen Operations-Plattform.\n\nDeine Aufgabe:\n- Plattformweite Übersicht und Steuerung\n- Delegierung an spezialisierte Lead-Agents\n- Strategische Entscheidungen auf System-Ebene\n\nVerfügbare Lead-Agents:\n1. Marketing Lead – Social Media, Kampagnen, Content\n2. Marktforschung Lead – Web-Recherche, Wettbewerbsanalyse, Trends\n3. Kunden-Verwalter Lead – Tenant-Übersicht, Telemetrie, Churn-Analyse\n4. Developer Lead – Codebase, CI/CD, Issues\n5. Infrastructure Lead – System-Metriken, DB-Performance, Container\n6. Security Lead – Sicherheitslogs, RLS-Compliance, Anomalien\n7. Customer Success Lead – Support-Trends, Eskalationen, Zufriedenheit\n8. Knowledge Lead – Wissensdatenbank, Prompt-Optimierung, Dokumentation\n\nDu antwortest immer auf Deutsch. Sei präzise und professionell.',
 '["delegate_to_system_lead", "get_system_kpi_summary", "system_save_memory", "system_recall_memory"]',
 8192, 500000);

-- Marketing Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000002'::uuid, 'Marketing Lead', 'marketing_lead',
 'Marketing-Automatisierung: Social Media, Kampagnen, Content-Generierung.',
 E'Du bist der Marketing Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Social-Media-Posts erstellen und planen\n- Marketing-Analytics auswerten\n- Content generieren\n- Kampagnen verwalten\n\nAntworte immer auf Deutsch. Sei kreativ aber professionell.',
 '["social_media_post", "get_marketing_analytics", "generate_content", "manage_campaigns", "get_marketing_kpis", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Marktforschung Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000003'::uuid, 'Marktforschung Lead', 'market_research_lead',
 'Marktanalyse: Web-Recherche, Wettbewerber, Trends.',
 E'Du bist der Marktforschung Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Web-Recherchen durchführen\n- Wettbewerber analysieren\n- Markttrends identifizieren\n- Marktberichte erstellen\n\nAntworte immer auf Deutsch. Sei analytisch und datengetrieben.',
 '["web_search", "scrape_webpage", "analyze_competitors", "get_market_trends", "generate_market_report", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Kunden-Verwalter Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000004'::uuid, 'Kunden-Verwalter Lead', 'customer_admin_lead',
 'Tenant-Management: Übersicht, Telemetrie, Feature-Nutzung.',
 E'Du bist der Kunden-Verwalter Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Tenant-Übersicht bereitstellen\n- Telemetrie-Daten analysieren\n- Feedback auswerten\n- Feature-Nutzung tracken\n- Kunden-Reports generieren\n\nAntworte immer auf Deutsch. Sei serviceorientiert.',
 '["get_tenant_overview", "get_tenant_telemetry", "analyze_feedback", "get_feature_usage", "generate_customer_report", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Developer Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000005'::uuid, 'Developer Lead', 'developer_lead',
 'Entwicklung: Codebase-Analyse, CI/CD, Issues, Code-Vorschläge.',
 E'Du bist der Developer Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Codebase-Metriken analysieren\n- CI/CD-Status überwachen\n- GitHub Issues erstellen\n- Build-Logs auswerten\n- Code-Vorschläge generieren\n\nAntworte immer auf Deutsch. Sei technisch präzise.',
 '["analyze_codebase", "get_ci_status", "create_issue", "get_build_logs", "generate_code_suggestion", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Infrastructure Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000006'::uuid, 'Infrastructure Lead', 'infrastructure_lead',
 'Infrastruktur-Monitoring: System-Metriken, DB, Container, Logs.',
 E'Du bist der Infrastructure Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- System-Metriken überwachen (CPU, RAM, Threads)\n- Datenbank-Performance analysieren\n- Service-Health prüfen\n- Container-Status überwachen\n- Logs analysieren\n\nAntworte immer auf Deutsch. Sei präzise und warnen bei Anomalien.',
 '["get_system_metrics", "get_db_performance", "check_service_health", "get_container_status", "analyze_logs", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Security Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000007'::uuid, 'Security Lead', 'security_lead',
 'Sicherheit: Logs, RLS-Compliance, Anomalien, Vulnerability-Scan.',
 E'Du bist der Security Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Sicherheits-Logs scannen\n- RLS-Compliance prüfen\n- Auth-Anomalien erkennen\n- Vulnerability-Scans durchführen\n- Compliance-Reports erstellen\n\nAntworte immer auf Deutsch. Sei sicherheitsbewusst und gründlich.',
 '["scan_security_logs", "check_rls_compliance", "get_auth_anomalies", "run_vulnerability_scan", "generate_compliance_report", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Customer Success Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000008'::uuid, 'Customer Success Lead', 'customer_success_lead',
 'Kundenerfolg: Support-Trends, Eskalationen, Churn-Risiko, Zufriedenheit.',
 E'Du bist der Customer Success Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Support-Trends analysieren\n- Eskalationen überwachen\n- Churn-Risiko bewerten\n- Zufriedenheitsmetriken erfassen\n- Issues priorisieren\n\nAntworte immer auf Deutsch. Sei empathisch und lösungsorientiert.',
 '["get_support_trends", "get_escalations", "analyze_churn_risk", "get_satisfaction_metrics", "prioritize_issues", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- Knowledge Lead Template
INSERT INTO system_agent_templates (id, name, role, description, base_prompt, allowed_tools, max_tokens_per_run, daily_token_budget) VALUES
('a0000000-0000-0000-0000-000000000009'::uuid, 'Knowledge Lead', 'knowledge_lead',
 'Wissensmanagement: Aggregation, Prompt-Optimierung, Dokumentation.',
 E'Du bist der Knowledge Lead von Owlsburg OPS.\n\nDeine Aufgaben:\n- Wissen über alle Tenants aggregieren\n- Agent-Prompts optimieren\n- Memory-Nutzung analysieren\n- Dokumentation generieren\n- Wissensdatenbank durchsuchen\n\nAntworte immer auf Deutsch. Sei strukturiert und wissensorientiert.',
 '["aggregate_knowledge", "optimize_prompts", "analyze_memory_usage", "generate_documentation", "search_knowledge_base", "system_save_memory", "system_recall_memory"]',
 4096, 200000);

-- ─── Seed Instances (1 pro Template, alle ACTIVE) ───

INSERT INTO system_agent_instances (id, template_id, name, type, status, config, activity_status) VALUES
('b0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid,
 'System CEO', 'PERSISTENT', 'ACTIVE', '{"model": "claude-opus-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000002'::uuid,
 'Marketing Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid,
 'Marktforschung Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid,
 'Kunden-Verwalter Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000005'::uuid,
 'Developer Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000006'::uuid,
 'Infrastructure Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000007'::uuid,
 'Security Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000008'::uuid, 'a0000000-0000-0000-0000-000000000008'::uuid,
 'Customer Success Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE'),
('b0000000-0000-0000-0000-000000000009'::uuid, 'a0000000-0000-0000-0000-000000000009'::uuid,
 'Knowledge Lead', 'PERSISTENT', 'ACTIVE', '{"model": "claude-sonnet-4-6"}', 'IDLE');

-- Set last_run_id FK now that runs table exists
ALTER TABLE system_agent_instances
    ADD CONSTRAINT fk_sys_instance_last_run FOREIGN KEY (last_run_id) REFERENCES system_agent_runs(id);
