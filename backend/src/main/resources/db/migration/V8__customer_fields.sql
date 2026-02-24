ALTER TABLE customers ADD COLUMN customer_number VARCHAR(50);
ALTER TABLE customers ADD COLUMN short_name VARCHAR(100);
CREATE UNIQUE INDEX idx_customers_number_tenant ON customers(tenant_id, customer_number);
