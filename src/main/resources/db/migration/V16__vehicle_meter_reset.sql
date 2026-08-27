CREATE TABLE vehicle_meter_reset (
    reset_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    reading_type VARCHAR(30) NOT NULL,
    from_epoch INTEGER NOT NULL,
    to_epoch INTEGER NOT NULL,
    last_reading_value NUMERIC(19,3) NOT NULL,
    new_meter_value NUMERIC(19,3) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vehicle_meter_reset_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT fk_vehicle_meter_reset_created_by FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT chk_vehicle_meter_reset_type CHECK (reading_type IN ('ODOMETER', 'ENGINE_HOURS')),
    CONSTRAINT chk_vehicle_meter_reset_epochs CHECK (to_epoch = from_epoch + 1 AND from_epoch >= 0),
    CONSTRAINT chk_vehicle_meter_reset_last_value CHECK (last_reading_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_new_value CHECK (new_meter_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_reason CHECK (LENGTH(TRIM(reason)) > 0)
);

CREATE INDEX idx_vehicle_meter_reset_lookup
    ON vehicle_meter_reset(vehicle_id, reading_type, effective_at DESC);