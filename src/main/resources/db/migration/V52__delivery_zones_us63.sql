-- V52__delivery_zones_us63.sql
-- US-63: Manage Delivery Zones

CREATE TABLE delivery_zone (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    zone_code VARCHAR(30) NOT NULL,
    zone_name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    zone_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    serviceable BOOLEAN NOT NULL DEFAULT TRUE,
    daily_capacity INT,
    depot_location_id UUID,
    min_latitude DOUBLE PRECISION NOT NULL,
    max_latitude DOUBLE PRECISION NOT NULL,
    min_longitude DOUBLE PRECISION NOT NULL,
    max_longitude DOUBLE PRECISION NOT NULL,
    boundary_geojson JSONB NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(80) NOT NULL,
    CONSTRAINT uk_delivery_zone_tenant_code UNIQUE (tenant_id, zone_code),
    CONSTRAINT uk_delivery_zone_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_delivery_zone_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_delivery_zone_type CHECK (zone_type IN ('URBAN_DENSE', 'SUBURBAN', 'RURAL', 'SPECIAL_SECURITY')),
    CONSTRAINT ck_delivery_zone_capacity CHECK (daily_capacity IS NULL OR daily_capacity >= 0),
    CONSTRAINT ck_delivery_zone_lat CHECK (min_latitude >= -90.0 AND max_latitude <= 90.0 AND min_latitude <= max_latitude),
    CONSTRAINT ck_delivery_zone_lon CHECK (min_longitude >= -180.0 AND max_longitude <= 180.0 AND min_longitude <= max_longitude)
);

CREATE INDEX idx_delivery_zone_tenant_status ON delivery_zone(tenant_id, status);
CREATE INDEX idx_delivery_zone_tenant_serviceable ON delivery_zone(tenant_id, serviceable);
CREATE INDEX idx_delivery_zone_bbox ON delivery_zone(tenant_id, min_latitude, max_latitude, min_longitude, max_longitude);

-- Optional snapshot delivery_zone_id on delivery_order with same-tenant integrity
ALTER TABLE delivery_order ADD COLUMN delivery_zone_id UUID;

ALTER TABLE delivery_order ADD CONSTRAINT fk_delivery_order_zone_tenant
    FOREIGN KEY (delivery_zone_id, tenant_id)
    REFERENCES delivery_zone(id, tenant_id);

CREATE INDEX idx_delivery_order_tenant_zone ON delivery_order(tenant_id, delivery_zone_id);

-- RBAC Permissions for Delivery Zones
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_ZONE_CREATE', 'Create delivery zones', TRUE),
    ('DELIVERY_ZONE_VIEW', 'View delivery zones', TRUE),
    ('DELIVERY_ZONE_UPDATE', 'Update delivery zones', TRUE),
    ('DELIVERY_ZONE_ACTIVATE', 'Activate or deactivate delivery zones', TRUE),
    ('DELIVERY_ZONE_OVERRIDE', 'Manually override delivery zone assignments', TRUE)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
