-- US-64 Delivery Slots Schema & RBAC Migration

-- 1. Create delivery_slot table
CREATE TABLE delivery_slot (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_zone_id UUID NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    max_capacity INTEGER NOT NULL CHECK (max_capacity > 0),
    reserved_capacity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_capacity >= 0),
    cutoff_time TIMESTAMPTZ,
    buffer_minutes INTEGER NOT NULL DEFAULT 0 CHECK (buffer_minutes >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT chk_slot_time_order CHECK (start_time < end_time),
    CONSTRAINT uk_delivery_slot_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_delivery_slot_zone_tenant FOREIGN KEY (delivery_zone_id, tenant_id)
        REFERENCES delivery_zone (id, tenant_id) ON DELETE RESTRICT
);

-- Index for searching slots by zone and date
CREATE INDEX idx_delivery_slot_zone_date ON delivery_slot (tenant_id, delivery_zone_id, slot_date, status);
CREATE INDEX idx_delivery_slot_date_status ON delivery_slot (tenant_id, slot_date, status);

-- 2. Create delivery_slot_reservation table
CREATE TABLE delivery_slot_reservation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_slot_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reserved_at TIMESTAMPTZ NOT NULL,
    reserved_by VARCHAR(255) NOT NULL,
    released_at TIMESTAMPTZ,
    released_by VARCHAR(255),
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_slot_reservation_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_slot_res_slot_tenant FOREIGN KEY (delivery_slot_id, tenant_id)
        REFERENCES delivery_slot (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_slot_res_order_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT
);

-- Unique index to ensure only one ACTIVE reservation exists per delivery order per tenant
CREATE UNIQUE INDEX uk_active_order_slot_reservation ON delivery_slot_reservation (tenant_id, delivery_order_id)
    WHERE status = 'ACTIVE';

-- 3. Add delivery_slot_id column to delivery_order with composite tenant foreign key
ALTER TABLE delivery_order ADD COLUMN delivery_slot_id UUID;
ALTER TABLE delivery_order ADD CONSTRAINT fk_delivery_order_slot_tenant
    FOREIGN KEY (delivery_slot_id, tenant_id) REFERENCES delivery_slot (id, tenant_id) ON DELETE SET NULL;

-- 4. Seed RBAC permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_SLOT_VIEW', 'View delivery slots and capacity matrices', true),
    ('DELIVERY_SLOT_CREATE', 'Create delivery slots', true),
    ('DELIVERY_SLOT_UPDATE', 'Update delivery slot window, capacity, cutoff, or buffer', true),
    ('DELIVERY_SLOT_ACTIVATE', 'Activate, deactivate, or close delivery slots', true),
    ('DELIVERY_SLOT_ASSIGN', 'Assign delivery orders to slots and release reservations', true),
    ('DELIVERY_SLOT_OVERRIDE', 'Overbook delivery slots exceeding maximum capacity', true)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
