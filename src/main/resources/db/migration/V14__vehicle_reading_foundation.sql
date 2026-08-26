CREATE TABLE vehicle_reading (
    reading_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    reading_type VARCHAR(30) NOT NULL,
    value NUMERIC(19,3) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    meter_epoch INTEGER NOT NULL DEFAULT 0,
    source_type VARCHAR(30) NOT NULL,
    source_reference_id UUID,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL,
    correction_of_reading_id UUID,
    correction_reason TEXT,
    idempotency_key VARCHAR(160),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vehicle_reading_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT fk_vehicle_reading_created_by FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT fk_vehicle_reading_correction FOREIGN KEY (correction_of_reading_id)
        REFERENCES vehicle_reading(reading_id),
    CONSTRAINT chk_vehicle_reading_type CHECK (reading_type IN ('ODOMETER', 'ENGINE_HOURS')),
    CONSTRAINT chk_vehicle_reading_unit CHECK (unit IN ('KILOMETER', 'HOUR')),
    CONSTRAINT chk_vehicle_reading_type_unit CHECK (
        (reading_type = 'ODOMETER' AND unit = 'KILOMETER') OR
        (reading_type = 'ENGINE_HOURS' AND unit = 'HOUR')
    ),
    CONSTRAINT chk_vehicle_reading_value CHECK (value >= 0),
    CONSTRAINT chk_vehicle_reading_epoch CHECK (meter_epoch >= 0),
    CONSTRAINT chk_vehicle_reading_source CHECK (source_type IN (
        'MANUAL', 'TRIP_START', 'TRIP_END', 'FUEL_ISSUE', 'BASELINE', 'METER_RESET',
        'TELEMATICS', 'MAINTENANCE'
    )),
    CONSTRAINT chk_vehicle_reading_source_reference CHECK (
        (source_type = 'MANUAL' AND source_reference_id IS NULL) OR
        (source_type <> 'MANUAL' AND source_reference_id IS NOT NULL)
    ),
    CONSTRAINT chk_vehicle_reading_correction_reason CHECK (
        (correction_of_reading_id IS NULL AND correction_reason IS NULL) OR
        (correction_of_reading_id IS NOT NULL AND correction_reason IS NOT NULL
            AND LENGTH(TRIM(correction_reason)) > 0)
    )
);

CREATE INDEX idx_vehicle_reading_chronology
    ON vehicle_reading(vehicle_id, reading_type, meter_epoch, recorded_at DESC);

CREATE INDEX idx_vehicle_reading_source
    ON vehicle_reading(source_type, source_reference_id);

CREATE INDEX idx_vehicle_reading_correction
    ON vehicle_reading(correction_of_reading_id);

CREATE UNIQUE INDEX uq_vehicle_reading_idempotency
    ON vehicle_reading(idempotency_key);

CREATE UNIQUE INDEX uq_vehicle_reading_one_correction
    ON vehicle_reading(correction_of_reading_id);

CREATE INDEX idx_vehicle_reading_source_identity ON vehicle_reading(source_type, source_reference_id);
