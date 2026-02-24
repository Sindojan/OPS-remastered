-- Tenant configuration extensions
ALTER TABLE public.tenants
    ADD COLUMN IF NOT EXISTS logo_url        VARCHAR(500),
    ADD COLUMN IF NOT EXISTS contact_email   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_phone   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS address         VARCHAR(500),
    ADD COLUMN IF NOT EXISTS city            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS postal_code     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS country         VARCHAR(100) DEFAULT 'Deutschland',
    ADD COLUMN IF NOT EXISTS website         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS vat_id          VARCHAR(50);

-- User notification settings
CREATE TABLE IF NOT EXISTS public.user_notification_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES public.tenants(id),
    user_id                 UUID NOT NULL REFERENCES public.users(id),
    agent_run_completed     BOOLEAN NOT NULL DEFAULT true,
    agent_run_failed        BOOLEAN NOT NULL DEFAULT true,
    stock_below_minimum     BOOLEAN NOT NULL DEFAULT true,
    machine_incident        BOOLEAN NOT NULL DEFAULT true,
    job_overdue             BOOLEAN NOT NULL DEFAULT true,
    absence_request         BOOLEAN NOT NULL DEFAULT true,
    inbox_new_message       BOOLEAN NOT NULL DEFAULT true,
    in_app                  BOOLEAN NOT NULL DEFAULT true,
    email_notifications     BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(tenant_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_notification_settings_tenant ON public.user_notification_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_notification_settings_user ON public.user_notification_settings(user_id);

-- RLS policy for notification settings
ALTER TABLE public.user_notification_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY user_notification_settings_tenant_isolation ON public.user_notification_settings
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
