-- US-66 Delivery Batches Schema & RBAC Migration

-- 1. Create delivery_batch_counter table (for BAT-YYYY-NNNNNN generation)
CREATE TABLE delivery_batch_counter (
    tenant_id UUID NOT NULL,
    calendar_year INTEGER NOT NULL CHECK (calendar_year BETWEEN 1000 AND 9999),
    last_value INTEGER NOT NULL CHECK (last_value BETWEEN 1 AND 999999),
    PRIMARY KEY (tenant_id, calendar_year)
);

-- 2. Create delivery_batch table
CREATE TABLE delivery_batch (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    batch_code VARCHAR(40) NOT NULL,
    delivery_zone_id UUID NOT NULL,
    delivery_slot_id UUID,
    rider_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    max_batch_size INTEGER NOT NULL DEFAULT 5 CHECK (max_batch_size > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_delivery_batch_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_delivery_batch_code_tenant UNIQUE (tenant_id, batch_code),
    CONSTRAINT ck_delivery_batch_status CHECK (status IN ('DRAFT', 'READY', 'ASSIGNED', 'DISPATCHED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT fk_delivery_batch_zone_tenant FOREIGN KEY (delivery_zone_id, tenant_id)
        REFERENCES delivery_zone (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_batch_slot_tenant FOREIGN KEY (delivery_slot_id, tenant_id)
        REFERENCES delivery_slot (id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT fk_delivery_batch_rider_tenant FOREIGN KEY (rider_id, tenant_id)
        REFERENCES delivery_rider (id, tenant_id) ON DELETE SET NULL
);

CREATE INDEX idx_delivery_batch_tenant_status ON delivery_batch (tenant_id, status);
CREATE INDEX idx_delivery_batch_tenant_zone ON delivery_batch (tenant_id, delivery_zone_id);
CREATE INDEX idx_delivery_batch_tenant_slot ON delivery_batch (tenant_id, delivery_slot_id);
CREATE INDEX idx_delivery_batch_tenant_rider ON delivery_batch (tenant_id, rider_id);

-- 3. Create delivery_batch_order table
CREATE TABLE delivery_batch_order (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    batch_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    sequence_hint INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    added_at TIMESTAMPTZ NOT NULL,
    added_by VARCHAR(255) NOT NULL,
    removed_at TIMESTAMPTZ,
    removed_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_delivery_batch_order_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_delivery_batch_order_status CHECK (status IN ('ACTIVE', 'REMOVED', 'COMPLETED')),
    CONSTRAINT fk_delivery_batch_order_batch_tenant FOREIGN KEY (batch_id, tenant_id)
        REFERENCES delivery_batch (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_batch_order_order_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT
);

-- Unique index to guarantee that a DeliveryOrder belongs to at most one ACTIVE batch per tenant
CREATE UNIQUE INDEX uk_active_batch_order ON delivery_batch_order (tenant_id, delivery_order_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_batch_order_lookup ON delivery_batch_order (tenant_id, batch_id, status);

-- 4. Seed RBAC permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_BATCH_VIEW', 'View delivery batches, clusters, and member orders', true),
    ('DELIVERY_BATCH_CREATE', 'Create manual delivery batches and execute auto-clustering', true),
    ('DELIVERY_BATCH_UPDATE', 'Update delivery batch metadata and modify order memberships', true),
    ('DELIVERY_BATCH_ASSIGN', 'Assign or reassign riders to delivery batches', true),
    ('DELIVERY_BATCH_DISPATCH', 'Dispatch delivery batches for execution', true),
    ('DELIVERY_BATCH_CANCEL', 'Cancel or disband delivery batches', true)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
