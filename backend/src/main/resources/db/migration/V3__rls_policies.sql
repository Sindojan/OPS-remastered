-- =============================================================================
-- V3__rls_policies.sql – Row Level Security for tenant isolation
-- =============================================================================

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
  SELECT NULLIF(current_setting('app.current_tenant', true), '')::UUID;
$$ LANGUAGE sql STABLE;

DO $$
DECLARE
  t RECORD;
BEGIN
  FOR t IN
    SELECT table_name FROM information_schema.columns
    WHERE table_schema = 'public' AND column_name = 'tenant_id'
      AND table_name NOT IN ('users', 'tenants')
  LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.table_name);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', t.table_name);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON public.%I USING (tenant_id = current_tenant_id())',
      t.table_name
    );
  END LOOP;
END $$;
