-- =============================================================================
-- V25__drop_custom_enum_types.sql – Replace PostgreSQL custom enum types with VARCHAR
-- Hibernate sends enum values as VARCHAR which conflicts with custom enum column types.
-- =============================================================================

-- Jobs
ALTER TABLE jobs ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

-- Machines
ALTER TABLE machines ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

-- Employees
ALTER TABLE employees ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Agent Instances
ALTER TABLE agent_instances ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE agent_instances ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Agent Runs
ALTER TABLE agent_runs ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE agent_runs ALTER COLUMN trigger_type TYPE VARCHAR(20) USING trigger_type::text;

-- Agent Run Steps
ALTER TABLE agent_run_steps ALTER COLUMN step_type TYPE VARCHAR(20) USING step_type::text;

-- Stock Movements
ALTER TABLE stock_movements ALTER COLUMN movement_type TYPE VARCHAR(20) USING movement_type::text;

-- Conversations
ALTER TABLE conversations ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE conversations ALTER COLUMN source TYPE VARCHAR(20) USING source::text;
ALTER TABLE conversations ALTER COLUMN priority TYPE VARCHAR(20) USING priority::text;

-- Messages
ALTER TABLE messages ALTER COLUMN sender_type TYPE VARCHAR(20) USING sender_type::text;

-- Quality Checks
ALTER TABLE quality_checks ALTER COLUMN result TYPE VARCHAR(20) USING result::text;

-- Maintenance Schedules
ALTER TABLE maintenance_schedules ALTER COLUMN maintenance_type TYPE VARCHAR(20) USING maintenance_type::text;

-- Maintenance Records
ALTER TABLE maintenance_records ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Addresses
ALTER TABLE addresses ALTER COLUMN address_type TYPE VARCHAR(20) USING address_type::text;

-- Absences
ALTER TABLE absences ALTER COLUMN absence_type TYPE VARCHAR(20) USING absence_type::text;
ALTER TABLE absences ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Time Entries
ALTER TABLE time_entries ALTER COLUMN entry_type TYPE VARCHAR(20) USING entry_type::text;

-- Parts
ALTER TABLE parts ALTER COLUMN part_type TYPE VARCHAR(20) USING part_type::text;

-- BOM Versions
ALTER TABLE bom_versions ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Process Plan Versions
ALTER TABLE process_plan_versions ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Documents
ALTER TABLE documents ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Domain Events
ALTER TABLE domain_events ALTER COLUMN severity TYPE VARCHAR(20) USING severity::text;

-- Conversation Links
ALTER TABLE conversation_links ALTER COLUMN reference_type TYPE VARCHAR(20) USING reference_type::text;

-- Agent Incidents
ALTER TABLE agent_incidents ALTER COLUMN severity TYPE VARCHAR(20) USING severity::text;

-- Now drop all custom enum types
DROP TYPE IF EXISTS job_status CASCADE;
DROP TYPE IF EXISTS machine_status CASCADE;
DROP TYPE IF EXISTS employee_status CASCADE;
DROP TYPE IF EXISTS agent_instance_status CASCADE;
DROP TYPE IF EXISTS agent_run_status CASCADE;
DROP TYPE IF EXISTS stock_movement_type CASCADE;
DROP TYPE IF EXISTS conversation_status CASCADE;
DROP TYPE IF EXISTS quality_result CASCADE;
DROP TYPE IF EXISTS maintenance_type CASCADE;
DROP TYPE IF EXISTS maintenance_record_status CASCADE;
DROP TYPE IF EXISTS address_type CASCADE;
DROP TYPE IF EXISTS absence_type CASCADE;
DROP TYPE IF EXISTS absence_status CASCADE;
DROP TYPE IF EXISTS time_entry_type CASCADE;
DROP TYPE IF EXISTS part_type CASCADE;
DROP TYPE IF EXISTS version_status CASCADE;
DROP TYPE IF EXISTS sender_type CASCADE;
DROP TYPE IF EXISTS conversation_source CASCADE;
DROP TYPE IF EXISTS document_status CASCADE;
DROP TYPE IF EXISTS agent_step_type CASCADE;
DROP TYPE IF EXISTS agent_instance_type CASCADE;
DROP TYPE IF EXISTS trigger_type CASCADE;
DROP TYPE IF EXISTS reference_type CASCADE;
DROP TYPE IF EXISTS severity_level CASCADE;
DROP TYPE IF EXISTS conversation_priority CASCADE;
