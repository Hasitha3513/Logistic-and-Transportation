-- V24__trip_operational_events.sql
-- Table for trip operational events: checkpoints, delays, and incidents (US-13)

CREATE TABLE trip_operational_event (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    location_id UUID,
    location_description VARCHAR(255),
    checkpoint_type VARCHAR(32),
    delay_minutes INTEGER,
    reason VARCHAR(500),
    incident_severity VARCHAR(32),
    remarks TEXT,
    recorded_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_trip_operational_event_trip FOREIGN KEY (trip_id) REFERENCES trip (id),
    CONSTRAINT chk_trip_op_event_type CHECK (event_type IN ('CHECKPOINT', 'DELAY', 'INCIDENT')),
    CONSTRAINT chk_trip_op_delay_minutes CHECK (delay_minutes IS NULL OR delay_minutes > 0),
    CONSTRAINT chk_trip_op_checkpoint_type CHECK (
        (event_type = 'CHECKPOINT' AND checkpoint_type IS NOT NULL) OR
        (event_type <> 'CHECKPOINT' AND checkpoint_type IS NULL)
    ),
    CONSTRAINT chk_trip_op_incident_severity CHECK (
        (event_type = 'INCIDENT' AND incident_severity IS NOT NULL) OR
        (event_type <> 'INCIDENT' AND incident_severity IS NULL)
    )
);

CREATE INDEX idx_trip_op_event_trip ON trip_operational_event (trip_id, occurred_at ASC);
CREATE INDEX idx_trip_op_event_type ON trip_operational_event (event_type);

-- Seed permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('TRIP_LOG_VIEW', 'View trip operational events, checkpoints, delays, and incidents', TRUE),
    ('TRIP_LOG_MANAGE', 'Record trip checkpoints, delays, and operational incidents', TRUE);
