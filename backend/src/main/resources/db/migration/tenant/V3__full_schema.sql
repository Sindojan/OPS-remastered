-- =============================================================================
-- V3__full_schema.sql – Vollständiges Anwendungsschema
-- Alle Domänen-Tabellen, Enums, Indizes und Constraints
-- =============================================================================

-- ===================== ENUMS =====================

CREATE TYPE job_status AS ENUM ('DRAFT', 'RELEASED', 'IN_PRODUCTION', 'ON_HOLD', 'COMPLETED', 'CANCELLED');
CREATE TYPE machine_status AS ENUM ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'BLOCKED', 'DECOMMISSIONED');
CREATE TYPE employee_status AS ENUM ('ACTIVE', 'INACTIVE', 'ON_LEAVE');
CREATE TYPE agent_instance_status AS ENUM ('INACTIVE', 'ACTIVE', 'QUARANTINE', 'TERMINATED');
CREATE TYPE agent_run_status AS ENUM ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED');
CREATE TYPE stock_movement_type AS ENUM ('INBOUND', 'OUTBOUND', 'TRANSFER', 'CORRECTION');
CREATE TYPE conversation_status AS ENUM ('OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'ARCHIVED');
CREATE TYPE quality_result AS ENUM ('PASS', 'FAIL', 'PARTIAL');
CREATE TYPE maintenance_type AS ENUM ('TIME_BASED', 'HOURS_BASED');
CREATE TYPE maintenance_record_status AS ENUM ('PLANNED', 'IN_PROGRESS', 'DONE', 'SKIPPED');
CREATE TYPE address_type AS ENUM ('BILLING', 'SHIPPING', 'BOTH');
CREATE TYPE absence_type AS ENUM ('VACATION', 'SICK', 'OTHER');
CREATE TYPE absence_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE time_entry_type AS ENUM ('CLOCK_IN', 'CLOCK_OUT', 'JOB_START', 'JOB_END');
CREATE TYPE part_type AS ENUM ('PRODUCT', 'COMPONENT', 'RAW_MATERIAL');
CREATE TYPE version_status AS ENUM ('DRAFT', 'ACTIVE', 'ARCHIVED');
CREATE TYPE sender_type AS ENUM ('USER', 'AGENT', 'CUSTOMER');
CREATE TYPE conversation_source AS ENUM ('EMAIL', 'MANUAL', 'AGENT');
CREATE TYPE document_status AS ENUM ('ACTIVE', 'ARCHIVED', 'DELETED');
CREATE TYPE agent_step_type AS ENUM ('LLM_CALL', 'TOOL_CALL');
CREATE TYPE agent_instance_type AS ENUM ('PERSISTENT', 'EPHEMERAL');
CREATE TYPE trigger_type AS ENUM ('CHAT', 'BUTTON', 'EVENT', 'SCHEDULE');
CREATE TYPE reference_type AS ENUM ('JOB', 'PURCHASE_ORDER', 'MANUAL');
CREATE TYPE severity_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE conversation_priority AS ENUM ('LOW', 'NORMAL', 'HIGH', 'URGENT');

-- ===================== KUNDEN =====================

CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name    VARCHAR(255) NOT NULL,
    tax_id          VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE customer_contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    position        VARCHAR(100),
    is_primary      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE customer_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    type            address_type NOT NULL,
    street          VARCHAR(255) NOT NULL,
    zip             VARCHAR(20) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT 'DE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE customer_price_groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    name            VARCHAR(100) NOT NULL,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    valid_from      DATE NOT NULL,
    valid_until     DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_customer_contacts_customer_id ON customer_contacts(customer_id);
CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses(customer_id);
CREATE INDEX idx_customer_price_groups_customer_id ON customer_price_groups(customer_id);

-- ===================== PRODUKTION =====================

CREATE TABLE shifts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    days_of_week    INTEGER[] NOT NULL DEFAULT '{1,2,3,4,5}',
    capacity_hours  NUMERIC(5,2),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE stations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    capacity_per_shift  INTEGER,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE station_shifts (
    station_id  UUID NOT NULL REFERENCES stations(id),
    shift_id    UUID NOT NULL REFERENCES shifts(id),
    PRIMARY KEY (station_id, shift_id)
);

CREATE TABLE jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_number          VARCHAR(50) NOT NULL UNIQUE,
    customer_id         UUID REFERENCES customers(id),
    title               VARCHAR(255) NOT NULL,
    status              job_status NOT NULL DEFAULT 'DRAFT',
    priority            INTEGER NOT NULL DEFAULT 0,
    quantity            INTEGER NOT NULL DEFAULT 1,
    deadline            TIMESTAMP WITH TIME ZONE,
    notes               TEXT,
    created_by          UUID REFERENCES users(id),
    assigned_station_id UUID REFERENCES stations(id),
    shift_id            UUID REFERENCES shifts(id),
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE job_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID NOT NULL REFERENCES jobs(id),
    from_status job_status,
    to_status   job_status NOT NULL,
    changed_by  UUID REFERENCES users(id),
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reason      TEXT
);

CREATE TABLE quality_checks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id),
    checked_by      UUID REFERENCES users(id),
    check_type      VARCHAR(100) NOT NULL,
    result          quality_result NOT NULL,
    defect_count    INTEGER NOT NULL DEFAULT 0,
    notes           TEXT,
    checked_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE quality_defects (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quality_check_id    UUID NOT NULL REFERENCES quality_checks(id),
    defect_type         VARCHAR(100) NOT NULL,
    description         TEXT,
    severity            severity_level NOT NULL DEFAULT 'MEDIUM'
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_deadline ON jobs(deadline);
CREATE INDEX idx_jobs_customer_id ON jobs(customer_id);
CREATE INDEX idx_jobs_assigned_station_id ON jobs(assigned_station_id);
CREATE INDEX idx_jobs_job_number ON jobs(job_number);
CREATE INDEX idx_job_status_history_job_id ON job_status_history(job_id);
CREATE INDEX idx_quality_checks_job_id ON quality_checks(job_id);
CREATE INDEX idx_quality_defects_quality_check_id ON quality_defects(quality_check_id);

-- ===================== MASCHINEN =====================

CREATE TABLE machines (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    machine_number      VARCHAR(50) NOT NULL UNIQUE,
    type                VARCHAR(100),
    station_id          UUID REFERENCES stations(id),
    status              machine_status NOT NULL DEFAULT 'AVAILABLE',
    capacity_per_hour   NUMERIC(8,2),
    manufacturer        VARCHAR(100),
    model               VARCHAR(100),
    serial_number       VARCHAR(100),
    purchase_date       DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE machine_availability (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id      UUID NOT NULL REFERENCES machines(id),
    shift_id        UUID NOT NULL REFERENCES shifts(id),
    available_hours NUMERIC(5,2) NOT NULL
);

CREATE TABLE maintenance_intervals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id          UUID NOT NULL REFERENCES machines(id),
    type                maintenance_type NOT NULL,
    interval_days       INTEGER,
    interval_hours      INTEGER,
    last_performed_at   TIMESTAMP WITH TIME ZONE,
    next_due_at         TIMESTAMP WITH TIME ZONE,
    description         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE maintenance_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id      UUID NOT NULL REFERENCES machines(id),
    interval_id     UUID REFERENCES maintenance_intervals(id),
    performed_by    UUID REFERENCES users(id),
    performed_at    TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    notes           TEXT,
    status          maintenance_record_status NOT NULL DEFAULT 'PLANNED',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE machine_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id      UUID NOT NULL REFERENCES machines(id),
    reported_by     UUID REFERENCES users(id),
    type            VARCHAR(100) NOT NULL,
    description     TEXT,
    severity        severity_level NOT NULL DEFAULT 'MEDIUM',
    reported_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT
);

CREATE INDEX idx_machines_station_id ON machines(station_id);
CREATE INDEX idx_machines_status ON machines(status);
CREATE INDEX idx_machine_availability_machine_id ON machine_availability(machine_id);
CREATE INDEX idx_maintenance_intervals_machine_id ON maintenance_intervals(machine_id);
CREATE INDEX idx_maintenance_records_machine_id ON maintenance_records(machine_id);
CREATE INDEX idx_machine_incidents_machine_id ON machine_incidents(machine_id);

-- ===================== MITARBEITER & ZEITERFASSUNG =====================

CREATE TABLE employees (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES users(id),
    employee_number     VARCHAR(50) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(50),
    role                VARCHAR(50) NOT NULL DEFAULT 'WORKER',
    status              employee_status NOT NULL DEFAULT 'ACTIVE',
    hire_date           DATE,
    station_id          UUID REFERENCES stations(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE employee_qualifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employees(id),
    qualification   VARCHAR(255) NOT NULL,
    certified_at    DATE,
    expires_at      DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE employee_shifts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employees(id),
    shift_id        UUID NOT NULL REFERENCES shifts(id),
    valid_from      DATE NOT NULL,
    valid_until     DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE time_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employees(id),
    type            time_entry_type NOT NULL,
    job_id          UUID REFERENCES jobs(id),
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE time_corrections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    time_entry_id   UUID NOT NULL REFERENCES time_entries(id),
    corrected_by    UUID NOT NULL REFERENCES users(id),
    old_timestamp   TIMESTAMP WITH TIME ZONE NOT NULL,
    new_timestamp   TIMESTAMP WITH TIME ZONE NOT NULL,
    reason          TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE absences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employees(id),
    type            absence_type NOT NULL,
    from_date       DATE NOT NULL,
    to_date         DATE NOT NULL,
    status          absence_status NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_user_id ON employees(user_id);
CREATE INDEX idx_employees_station_id ON employees(station_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employee_qualifications_employee_id ON employee_qualifications(employee_id);
CREATE INDEX idx_employee_shifts_employee_id ON employee_shifts(employee_id);
CREATE INDEX idx_time_entries_employee_id ON time_entries(employee_id);
CREATE INDEX idx_time_entries_timestamp ON time_entries(timestamp);
CREATE INDEX idx_time_entries_job_id ON time_entries(job_id);
CREATE INDEX idx_time_corrections_time_entry_id ON time_corrections(time_entry_id);
CREATE INDEX idx_absences_employee_id ON absences(employee_id);

-- ===================== LAGER & MATERIAL =====================

CREATE TABLE units (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL UNIQUE,
    abbreviation    VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE article_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    parent_id       UUID REFERENCES article_categories(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_number  VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category_id     UUID REFERENCES article_categories(id),
    unit_id         UUID REFERENCES units(id),
    min_stock       NUMERIC(12,3) NOT NULL DEFAULT 0,
    reorder_point   NUMERIC(12,3) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE warehouses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    location        VARCHAR(255),
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE warehouse_locations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id    UUID NOT NULL REFERENCES warehouses(id),
    aisle           VARCHAR(20),
    rack            VARCHAR(20),
    shelf           VARCHAR(20),
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE stock (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id              UUID NOT NULL REFERENCES articles(id),
    warehouse_location_id   UUID NOT NULL REFERENCES warehouse_locations(id),
    quantity                NUMERIC(12,3) NOT NULL DEFAULT 0,
    reserved_quantity       NUMERIC(12,3) NOT NULL DEFAULT 0,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id      UUID NOT NULL REFERENCES articles(id),
    from_location_id UUID REFERENCES warehouse_locations(id),
    to_location_id  UUID REFERENCES warehouse_locations(id),
    quantity        NUMERIC(12,3) NOT NULL,
    type            stock_movement_type NOT NULL,
    reference_type  reference_type,
    reference_id    UUID,
    performed_by    UUID REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE suppliers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    contact_name    VARCHAR(200),
    email           VARCHAR(255),
    phone           VARCHAR(50),
    address         TEXT,
    tax_id          VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE supplier_articles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id             UUID NOT NULL REFERENCES suppliers(id),
    article_id              UUID NOT NULL REFERENCES articles(id),
    supplier_article_number VARCHAR(100),
    lead_time_days          INTEGER,
    min_order_quantity      NUMERIC(12,3),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE supplier_price_lists (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_article_id UUID NOT NULL REFERENCES supplier_articles(id),
    price               NUMERIC(12,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'EUR',
    valid_from          DATE NOT NULL,
    valid_until         DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_articles_category_id ON articles(category_id);
CREATE INDEX idx_articles_article_number ON articles(article_number);
CREATE INDEX idx_article_categories_parent_id ON article_categories(parent_id);
CREATE INDEX idx_warehouse_locations_warehouse_id ON warehouse_locations(warehouse_id);
CREATE INDEX idx_stock_article_id ON stock(article_id);
CREATE INDEX idx_stock_warehouse_location_id ON stock(warehouse_location_id);
CREATE INDEX idx_stock_movements_article_id ON stock_movements(article_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at);
CREATE INDEX idx_supplier_articles_supplier_id ON supplier_articles(supplier_id);
CREATE INDEX idx_supplier_articles_article_id ON supplier_articles(article_id);
CREATE INDEX idx_supplier_price_lists_supplier_article_id ON supplier_price_lists(supplier_article_id);

-- ===================== STÜCKLISTEN & KALKULATION =====================

CREATE TABLE parts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    part_number     VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            part_type NOT NULL,
    unit_id         UUID REFERENCES units(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE bom_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id         UUID NOT NULL REFERENCES parts(id),
    version_number  INTEGER NOT NULL DEFAULT 1,
    status          version_status NOT NULL DEFAULT 'DRAFT',
    valid_from      DATE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE bom_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_version_id      UUID NOT NULL REFERENCES bom_versions(id),
    component_part_id   UUID NOT NULL REFERENCES parts(id),
    quantity            NUMERIC(12,4) NOT NULL,
    unit_id             UUID REFERENCES units(id),
    position            INTEGER NOT NULL,
    notes               TEXT
);

CREATE TABLE process_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id         UUID NOT NULL REFERENCES parts(id),
    version_number  INTEGER NOT NULL DEFAULT 1,
    name            VARCHAR(255) NOT NULL,
    status          version_status NOT NULL DEFAULT 'DRAFT',
    valid_from      DATE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE process_steps (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_plan_id         UUID NOT NULL REFERENCES process_plans(id),
    step_number             INTEGER NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    station_id              UUID REFERENCES stations(id),
    machine_id              UUID REFERENCES machines(id),
    setup_time_minutes      INTEGER NOT NULL DEFAULT 0,
    processing_time_minutes INTEGER NOT NULL DEFAULT 0,
    notes                   TEXT
);

CREATE TABLE cost_rates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL,
    reference_id    UUID NOT NULL,
    rate_per_hour   NUMERIC(10,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    valid_from      DATE NOT NULL,
    valid_until     DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE calculations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id         UUID NOT NULL REFERENCES parts(id),
    bom_version_id  UUID REFERENCES bom_versions(id),
    process_plan_id UUID REFERENCES process_plans(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    material_cost   NUMERIC(12,4) NOT NULL DEFAULT 0,
    labor_cost      NUMERIC(12,4) NOT NULL DEFAULT 0,
    overhead_cost   NUMERIC(12,4) NOT NULL DEFAULT 0,
    total_cost      NUMERIC(12,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    calculated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    calculated_by   UUID REFERENCES users(id)
);

CREATE TABLE job_calculations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              UUID NOT NULL REFERENCES jobs(id),
    calculation_id      UUID NOT NULL REFERENCES calculations(id),
    actual_material_cost NUMERIC(12,4),
    actual_labor_cost   NUMERIC(12,4),
    actual_total_cost   NUMERIC(12,4),
    variance_percent    NUMERIC(8,2),
    finalized_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_parts_part_number ON parts(part_number);
CREATE INDEX idx_bom_versions_part_id ON bom_versions(part_id);
CREATE INDEX idx_bom_items_bom_version_id ON bom_items(bom_version_id);
CREATE INDEX idx_bom_items_component_part_id ON bom_items(component_part_id);
CREATE INDEX idx_process_plans_part_id ON process_plans(part_id);
CREATE INDEX idx_process_steps_process_plan_id ON process_steps(process_plan_id);
CREATE INDEX idx_cost_rates_reference_id ON cost_rates(reference_id);
CREATE INDEX idx_calculations_part_id ON calculations(part_id);
CREATE INDEX idx_job_calculations_job_id ON job_calculations(job_id);
CREATE INDEX idx_job_calculations_calculation_id ON job_calculations(calculation_id);

-- ===================== INBOX & SUPPORT =====================

CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject         VARCHAR(500) NOT NULL,
    customer_id     UUID REFERENCES customers(id),
    status          conversation_status NOT NULL DEFAULT 'OPEN',
    priority        conversation_priority NOT NULL DEFAULT 'NORMAL',
    sla_due_at      TIMESTAMP WITH TIME ZONE,
    assigned_to     UUID REFERENCES users(id),
    source          conversation_source NOT NULL DEFAULT 'MANUAL',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE conversation_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    content         TEXT NOT NULL,
    sender_type     sender_type NOT NULL,
    sender_id       UUID,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE conversation_tags (
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    tag             VARCHAR(100) NOT NULL,
    PRIMARY KEY (conversation_id, tag)
);

CREATE TABLE conversation_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    linked_type     VARCHAR(50) NOT NULL,
    linked_id       UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_customer_id ON conversations(customer_id);
CREATE INDEX idx_conversations_assigned_to ON conversations(assigned_to);
CREATE INDEX idx_conversation_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX idx_conversation_links_conversation_id ON conversation_links(conversation_id);

-- ===================== DOKUMENTE & WISSEN =====================

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    file_key        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100),
    file_size_bytes BIGINT,
    version         INTEGER NOT NULL DEFAULT 1,
    status          document_status NOT NULL DEFAULT 'ACTIVE',
    uploaded_by     UUID REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE document_tags (
    document_id UUID NOT NULL REFERENCES documents(id),
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (document_id, tag)
);

CREATE TABLE document_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    linked_type     VARCHAR(50) NOT NULL,
    linked_id       UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_document_links_document_id ON document_links(document_id);

-- ===================== AGENT INFRASTRUCTURE =====================

CREATE TABLE agent_templates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    role                VARCHAR(100) NOT NULL,
    description         TEXT,
    base_prompt         TEXT,
    allowed_tools       JSONB NOT NULL DEFAULT '[]',
    trigger_types       JSONB NOT NULL DEFAULT '[]',
    max_tokens_per_run  INTEGER NOT NULL DEFAULT 4096,
    daily_token_budget  INTEGER NOT NULL DEFAULT 100000,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version             INTEGER NOT NULL DEFAULT 1,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE agent_instances (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id         UUID NOT NULL REFERENCES agent_templates(id),
    name                VARCHAR(255) NOT NULL,
    parent_instance_id  UUID REFERENCES agent_instances(id),
    type                agent_instance_type NOT NULL DEFAULT 'PERSISTENT',
    status              agent_instance_status NOT NULL DEFAULT 'INACTIVE',
    tenant_id           VARCHAR(63),
    config              JSONB DEFAULT '{}',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    terminated_at       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE agent_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES agent_instances(id),
    trigger_type    trigger_type NOT NULL,
    trigger_source  VARCHAR(255),
    input_context   JSONB,
    output          JSONB,
    status          agent_run_status NOT NULL DEFAULT 'PENDING',
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    cost_usd        NUMERIC(10,6) NOT NULL DEFAULT 0,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE agent_run_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES agent_runs(id),
    step_number     INTEGER NOT NULL,
    type            agent_step_type NOT NULL,
    tool_name       VARCHAR(255),
    input           JSONB,
    output          JSONB,
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    duration_ms     INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE agent_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES agent_instances(id),
    type            VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_agent_instances_template_id ON agent_instances(template_id);
CREATE INDEX idx_agent_instances_status ON agent_instances(status);
CREATE INDEX idx_agent_runs_instance_id ON agent_runs(instance_id);
CREATE INDEX idx_agent_runs_status ON agent_runs(status);
CREATE INDEX idx_agent_runs_started_at ON agent_runs(started_at);
CREATE INDEX idx_agent_run_steps_run_id ON agent_run_steps(run_id);
CREATE INDEX idx_agent_incidents_instance_id ON agent_incidents(instance_id);

-- ===================== EVENTS & SCHEDULER =====================

CREATE TABLE domain_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(255) NOT NULL,
    source_type     VARCHAR(100) NOT NULL,
    source_id       UUID,
    payload         JSONB NOT NULL DEFAULT '{}',
    processed       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE scheduled_triggers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL REFERENCES agent_instances(id),
    cron_expression VARCHAR(100) NOT NULL,
    last_run_at     TIMESTAMP WITH TIME ZONE,
    next_run_at     TIMESTAMP WITH TIME ZONE,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_domain_events_processed ON domain_events(processed);
CREATE INDEX idx_domain_events_event_type ON domain_events(event_type);
CREATE INDEX idx_domain_events_created_at ON domain_events(created_at);
CREATE INDEX idx_scheduled_triggers_instance_id ON scheduled_triggers(instance_id);
CREATE INDEX idx_scheduled_triggers_next_run_at ON scheduled_triggers(next_run_at);
