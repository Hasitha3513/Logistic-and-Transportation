-- V23__lubricant_logs.sql
-- Table for vehicle lubricant and fluid consumption logs (US-05)

CREATE TABLE lubricant_log (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    fluid_type VARCHAR(32) NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    odometer_km NUMERIC(12, 2),
    engine_hours NUMERIC(12, 2),
    vendor_id UUID,
    supplier_name VARCHAR(150),
    reference_number VARCHAR(100),
    remarks TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT fk_lubricant_log_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    CONSTRAINT chk_lubricant_quantity CHECK (quantity > 0),
    CONSTRAINT chk_lubricant_odometer CHECK (odometer_km IS NULL OR odometer_km >= 0),
    CONSTRAINT chk_lubricant_engine_hours CHECK (engine_hours IS NULL OR engine_hours >= 0)
);

CREATE INDEX idx_lubricant_log_vehicle ON lubricant_log (vehicle_id, recorded_at DESC);
CREATE INDEX idx_lubricant_log_fluid_type ON lubricant_log (fluid_type);
CREATE INDEX idx_lubricant_log_vendor ON lubricant_log (vendor_id);

-- Seed permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('LUBRICANT_LOG_VIEW', 'View vehicle lubricant and fluid consumption logs', TRUE),
    ('LUBRICANT_LOG_MANAGE', 'Record and manage vehicle lubricant and fluid consumption logs', TRUE);
