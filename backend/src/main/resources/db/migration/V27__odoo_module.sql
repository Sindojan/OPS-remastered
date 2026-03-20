-- =====================================================
-- V27: Odoo-Integration Module
-- =====================================================

-- 1. Modul registrieren
INSERT INTO modules (id, label, description, is_core, display_order) VALUES
  ('odoo', 'Odoo-Integration', 'Anbindung an Odoo 19+ ERP', FALSE, 10);

-- 2. Für bestehende Tenants deaktiviert anlegen
INSERT INTO tenant_modules (tenant_id, module_id, enabled)
SELECT t.id, 'odoo', FALSE FROM tenants t;

-- 3. odoo_config Tabelle
CREATE TABLE odoo_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  base_url VARCHAR(500) NOT NULL,
  database_name VARCHAR(200) NOT NULL,
  api_key_enc TEXT NOT NULL,
  odoo_version VARCHAR(20) NOT NULL DEFAULT '19.0',
  connection_status VARCHAR(30) NOT NULL DEFAULT 'UNCONFIGURED',
  last_connected_at TIMESTAMPTZ,
  settings JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(tenant_id)
);

CREATE INDEX idx_odoo_config_tenant ON odoo_config(tenant_id);

ALTER TABLE odoo_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY odoo_config_tenant_isolation ON odoo_config
  USING (tenant_id::text = current_setting('app.current_tenant', true));

-- 4. Odoo-Tools den Agents zuweisen
-- CEO bekommt odoo_search als generisches Power-Tool
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"odoo_search"'::jsonb
  ) sub
) WHERE role = 'ceo';

-- Supply Lead: odoo_get_products, odoo_get_stock, odoo_get_purchase_orders
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"odoo_get_products"'::jsonb
    UNION ALL SELECT '"odoo_get_stock"'::jsonb
    UNION ALL SELECT '"odoo_get_purchase_orders"'::jsonb
  ) sub
) WHERE role = 'supply_lead';

-- Production Lead: odoo_get_manufacturing
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"odoo_get_manufacturing"'::jsonb
  ) sub
) WHERE role = 'production_lead';

-- People Lead: odoo_get_employees
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"odoo_get_employees"'::jsonb
  ) sub
) WHERE role = 'people_lead';

-- Support Lead: odoo_get_partners, odoo_get_sale_orders
UPDATE agent_templates SET allowed_tools = (
  SELECT jsonb_agg(elem) FROM (
    SELECT jsonb_array_elements(allowed_tools) AS elem
    UNION ALL SELECT '"odoo_get_partners"'::jsonb
    UNION ALL SELECT '"odoo_get_sale_orders"'::jsonb
  ) sub
) WHERE role = 'support_lead';
