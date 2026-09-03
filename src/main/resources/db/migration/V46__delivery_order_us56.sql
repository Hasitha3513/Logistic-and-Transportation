CREATE TABLE delivery_number_counter (
    tenant_id UUID NOT NULL,
    calendar_year INTEGER NOT NULL CHECK (calendar_year BETWEEN 1000 AND 9999),
    last_value INTEGER NOT NULL CHECK (last_value BETWEEN 1 AND 999999),
    PRIMARY KEY (tenant_id, calendar_year)
);

CREATE TABLE delivery_order (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_number VARCHAR(15) NOT NULL,
    customer_id UUID NOT NULL,
    origin_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    service_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD'
        CHECK (service_type IN ('STANDARD', 'EXPRESS', 'SAME_DAY', 'SCHEDULED')),
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    instructions TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'READY_FOR_ASSIGNMENT')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uk_delivery_order_tenant_number UNIQUE (tenant_id, delivery_number),
    CONSTRAINT ck_delivery_order_locations CHECK (origin_location_id <> destination_location_id),
    CONSTRAINT ck_delivery_order_window CHECK (window_start <= window_end)
);

CREATE INDEX idx_delivery_order_tenant_status ON delivery_order(tenant_id, status);
CREATE INDEX idx_delivery_order_tenant_customer ON delivery_order(tenant_id, customer_id);
CREATE INDEX idx_delivery_order_tenant_window ON delivery_order(tenant_id, window_start, window_end);

INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_VIEW', 'View tenant-scoped Delivery Orders', TRUE),
    ('DELIVERY_CREATE', 'Create tenant-scoped Delivery Orders', TRUE),
    ('DELIVERY_UPDATE', 'Update tenant-scoped Delivery Order requirements', TRUE),
    ('DELIVERY_ASSIGN', 'Validate Delivery Order readiness for later assignment', TRUE);
