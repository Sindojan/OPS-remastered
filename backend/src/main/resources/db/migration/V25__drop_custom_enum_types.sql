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
ALTER TABLE agent_run_steps ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Stock Movements
ALTER TABLE stock_movements ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Conversations
ALTER TABLE conversations ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE conversations ALTER COLUMN source TYPE VARCHAR(20) USING source::text;
ALTER TABLE conversations ALTER COLUMN priority TYPE VARCHAR(20) USING priority::text;

-- Conversation Messages
ALTER TABLE conversation_messages ALTER COLUMN sender_type TYPE VARCHAR(20) USING sender_type::text;

-- Quality Checks
ALTER TABLE quality_checks ALTER COLUMN result TYPE VARCHAR(20) USING result::text;

-- Maintenance Intervals
ALTER TABLE maintenance_intervals ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Maintenance Records
ALTER TABLE maintenance_records ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Customer Addresses
ALTER TABLE customer_addresses ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Absences
ALTER TABLE absences ALTER COLUMN type TYPE VARCHAR(20) USING type::text;
ALTER TABLE absences ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Time Entries
ALTER TABLE time_entries ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Parts
ALTER TABLE parts ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- BOM Versions
ALTER TABLE bom_versions ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Process Plans
ALTER TABLE process_plans ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Documents
ALTER TABLE documents ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Conversation Links
ALTER TABLE conversation_links ALTER COLUMN linked_type TYPE VARCHAR(50) USING linked_type::text;

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
