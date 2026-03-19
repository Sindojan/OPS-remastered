-- =============================================================================
-- V26__fix_severity_columns_and_cleanup.sql
-- Fix columns that were DROPPED by DROP TYPE severity_level CASCADE in V25.
-- Also: convert chat_sessions/chat_messages timestamps to TIMESTAMPTZ for consistency.
-- =============================================================================

-- Quality Defects: severity was dropped by CASCADE – re-add as VARCHAR
ALTER TABLE quality_defects ADD COLUMN IF NOT EXISTS severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

-- Machine Incidents: severity was dropped by CASCADE – re-add as VARCHAR
ALTER TABLE machine_incidents ADD COLUMN IF NOT EXISTS severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

-- Chat Sessions: TIMESTAMP → TIMESTAMPTZ for timezone consistency
ALTER TABLE chat_sessions ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE chat_sessions ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- Chat Messages: TIMESTAMP → TIMESTAMPTZ
ALTER TABLE chat_messages ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- Reorder Requests: TIMESTAMP → TIMESTAMPTZ
ALTER TABLE reorder_requests ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE reorder_requests ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
