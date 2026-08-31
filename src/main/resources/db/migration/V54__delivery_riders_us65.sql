-- US-65 Delivery Riders Schema & RBAC Migration

-- 1. Create delivery_rider table
CREATE TABLE delivery_rider (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rider_code VARCHAR(40) NOT NULL,
    driver_id UUID NOT NULL,
    rider_type VARCHAR(30) NOT NULL DEFAULT 'FULL_TIME',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    primary_zone_id UUID NOT NULL,
    max_concurrent_deliveries INTEGER NOT NULL DEFAULT 5 CHECK (max_concurrent_deliveries > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_delivery_rider_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_delivery_rider_code_tenant UNIQUE (tenant_id, rider_code),
    CONSTRAINT fk_delivery_rider_zone_tenant FOREIGN KEY (primary_zone_id, tenant_id)
        REFERENCES delivery_zone (id, tenant_id) ON DELETE RESTRICT
);

-- Unique index to guarantee one active rider profile per driver per tenant
CREATE UNIQUE INDEX uk_active_driver_rider ON delivery_rider (tenant_id, driver_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_delivery_rider_zone ON delivery_rider (tenant_id, primary_zone_id, status);

-- 2. Create delivery_rider_zone table (Secondary eligible zones)
CREATE TABLE delivery_rider_zone (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rider_id UUID NOT NULL,
    delivery_zone_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    CONSTRAINT uk_delivery_rider_zone_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_delivery_rider_secondary_zone UNIQUE (tenant_id, rider_id, delivery_zone_id),
    CONSTRAINT fk_rider_zone_rider_tenant FOREIGN KEY (rider_id, tenant_id)
        REFERENCES delivery_rider (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_rider_zone_zone_tenant FOREIGN KEY (delivery_zone_id, tenant_id)
        REFERENCES delivery_zone (id, tenant_id) ON DELETE RESTRICT
);

CREATE INDEX idx_rider_zone_lookup ON delivery_rider_zone (tenant_id, delivery_zone_id);

-- 3. Create delivery_rider_shift table
CREATE TABLE delivery_rider_shift (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rider_id UUID NOT NULL,
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    delivery_slot_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    max_deliveries INTEGER DEFAULT 5 CHECK (max_deliveries > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT chk_rider_shift_time_order CHECK (start_time < end_time),
    CONSTRAINT uk_rider_shift_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_rider_shift_rider_tenant FOREIGN KEY (rider_id, tenant_id)
        REFERENCES delivery_rider (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_rider_shift_slot_tenant FOREIGN KEY (delivery_slot_id, tenant_id)
        REFERENCES delivery_slot (id, tenant_id) ON DELETE SET NULL
);

CREATE INDEX idx_rider_shift_date ON delivery_rider_shift (tenant_id, rider_id, shift_date, status);

-- 4. Create delivery_order_rider_assignment table (Historical assignments)
CREATE TABLE delivery_order_rider_assignment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    rider_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by VARCHAR(255) NOT NULL,
    unassigned_at TIMESTAMPTZ,
    unassigned_by VARCHAR(255),
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_order_rider_assignment_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_order_rider_order_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_rider_rider_tenant FOREIGN KEY (rider_id, tenant_id)
        REFERENCES delivery_rider (id, tenant_id) ON DELETE RESTRICT
);

-- Unique index to ensure only one ACTIVE rider assignment exists per delivery order per tenant
CREATE UNIQUE INDEX uk_active_delivery_order_rider ON delivery_order_rider_assignment (tenant_id, delivery_order_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_active_rider_assignments ON delivery_order_rider_assignment (tenant_id, rider_id, status);

-- 5. Add current_rider_id column to delivery_order with composite tenant foreign key
ALTER TABLE delivery_order ADD COLUMN current_rider_id UUID;
ALTER TABLE delivery_order ADD CONSTRAINT fk_delivery_order_rider_tenant
    FOREIGN KEY (current_rider_id, tenant_id) REFERENCES delivery_rider (id, tenant_id) ON DELETE SET NULL;

-- 6. Seed RBAC permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_RIDER_VIEW', 'View delivery rider roster, shifts, and availability', true),
    ('DELIVERY_RIDER_CREATE', 'Onboard new delivery riders and link driver profiles', true),
    ('DELIVERY_RIDER_UPDATE', 'Update delivery rider profile, zones, and shifts', true),
    ('DELIVERY_RIDER_ACTIVATE', 'Activate, deactivate, or suspend delivery riders', true),
    ('DELIVERY_RIDER_ASSIGN', 'Assign, reassign, and unassign riders on delivery orders', true),
    ('DELIVERY_RIDER_OVERRIDE', 'Override cross-zone or workload capacity limits during rider assignment', true)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
