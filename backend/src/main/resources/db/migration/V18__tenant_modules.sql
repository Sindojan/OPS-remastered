-- =====================================================
-- V18: Per-Tenant Feature-Module
-- =====================================================

-- Globale Modul-Registry (kein tenant_id, kein RLS)
CREATE TABLE modules (
    id VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    is_core BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0
);

-- Per-Tenant Modul-Aktivierung (mit RLS)
CREATE TABLE tenant_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    module_id VARCHAR(50) NOT NULL REFERENCES modules(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    enabled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, module_id)
);

CREATE INDEX idx_tenant_modules_tenant ON tenant_modules(tenant_id);

ALTER TABLE tenant_modules ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_modules_isolation ON tenant_modules
    USING (tenant_id::text = current_setting('app.current_tenant', true));

-- Seed 9 Module
INSERT INTO modules (id, label, description, is_core, display_order) VALUES
    ('core', 'Kern', 'Grundfunktionen: Authentifizierung, Agenten, Einstellungen, Berichte', TRUE, 0),
    ('production', 'Produktion', 'Aufträge, Stationen, Schichten, Qualitätsprüfungen', FALSE, 1),
    ('machines', 'Maschinen', 'Maschinenverwaltung, Wartung, Störungsmeldungen', FALSE, 2),
    ('inventory', 'Lager & Material', 'Artikel, Bestand, Bewegungen, Lieferanten', FALSE, 3),
    ('people', 'Personal', 'Mitarbeiter, Zeiterfassung, Abwesenheiten', FALSE, 4),
    ('customers', 'Kunden', 'Kundenverwaltung, Kontakte, Adressen, Preisgruppen', FALSE, 5),
    ('inbox', 'Posteingang', 'Konversationen, Nachrichten, Support', FALSE, 6),
    ('bom', 'Teile & Prozesse', 'Stücklisten, Arbeitspläne, Kalkulation', FALSE, 7),
    ('knowledge', 'Wissensdatenbank', 'Wissensartikel, Kategorien, Dokumentensuche', FALSE, 8);

-- Alle Module für bestehende Tenants aktivieren (Backward-Compat)
INSERT INTO tenant_modules (tenant_id, module_id, enabled)
SELECT t.id, m.id, TRUE FROM tenants t CROSS JOIN modules m;
