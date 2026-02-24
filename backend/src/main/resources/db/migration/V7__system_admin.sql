-- Erweitere tenants Tabelle
ALTER TABLE tenants ADD COLUMN slug VARCHAR(100) UNIQUE;
ALTER TABLE tenants ADD COLUMN plan VARCHAR(50) NOT NULL DEFAULT 'BASIC';
ALTER TABLE tenants ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE tenants ADD COLUMN suspended_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenants ADD COLUMN suspend_reason TEXT;

-- Setze slug für bestehenden Default-Tenant
UPDATE tenants SET slug = 'owlsburg-manufaktur' WHERE id = '00000000-0000-0000-0000-000000000001';

-- System Admin User (kein Tenant, tenant_id zeigt auf Default-Tenant als Dummy)
INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'philipp.ebert@strate-software', '$2b$12$OD2/GsZj89U5zIWMA130Nel5nDD7ATimOe29ld2wz.4uh/P5u9Noi', 'Philipp', 'Ebert', 'SYSTEM_ADMIN', true);
