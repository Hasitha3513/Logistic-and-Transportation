INSERT INTO app_permission (code, description, active) VALUES
    ('VEHICLE_READING_VIEW', 'View vehicle odometer and engine-hour readings', TRUE),
    ('VEHICLE_READING_CREATE', 'Create manual vehicle readings', TRUE),
    ('VEHICLE_READING_CORRECT', 'Correct recorded vehicle readings', TRUE),
    ('VEHICLE_READING_RESET_METER', 'Record physical vehicle meter replacement or reset', TRUE);

CREATE TABLE vehicle_meter_reset (
    reset_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    reading_type VARCHAR(30) NOT NULL,
    previous_reading_id UUID,
    previous_meter_value NUMERIC(19,3) NOT NULL,
    new_reading_id UUID NOT NULL,
    new_meter_value NUMERIC(19,3) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT NOT NULL,
    created_by UUID NOT NULL,
    approved_by UUID,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vehicle_meter_reset_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT fk_vehicle_meter_reset_created_by FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT fk_vehicle_meter_reset_approved_by FOREIGN KEY (approved_by) REFERENCES app_user(id),
    CONSTRAINT fk_vehicle_meter_reset_previous FOREIGN KEY (previous_reading_id) REFERENCES vehicle_reading(reading_id),
    CONSTRAINT fk_vehicle_meter_reset_new FOREIGN KEY (new_reading_id) REFERENCES vehicle_reading(reading_id),
    CONSTRAINT chk_vehicle_meter_reset_type CHECK (reading_type IN ('ODOMETER', 'ENGINE_HOURS')),
    CONSTRAINT chk_vehicle_meter_reset_prev_value CHECK (previous_meter_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_new_value CHECK (new_meter_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_reason CHECK (LENGTH(TRIM(reason)) > 0)
);

CREATE INDEX idx_vehicle_meter_reset_vehicle
    ON vehicle_meter_reset(vehicle_id, reading_type, effective_at DESC);

CREATE INDEX idx_vehicle_meter_reset_new_reading
    ON vehicle_meter_reset(new_reading_id);

ALTER TABLE vehicle_reading
    ADD CONSTRAINT chk_vehicle_reading_no_self_correction
    CHECK (correction_of_reading_id IS NULL OR reading_id <> correction_of_reading_id);
