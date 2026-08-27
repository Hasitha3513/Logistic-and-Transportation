CREATE TABLE maintenance_schedule (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    maintenance_type VARCHAR(64) NOT NULL,
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    description VARCHAR(500),
    service_provider VARCHAR(255),
    cost NUMERIC(12, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    CONSTRAINT chk_maintenance_schedule_dates CHECK (scheduled_end > scheduled_start)
);

CREATE INDEX idx_maint_sched_vehicle_status ON maintenance_schedule(vehicle_id, status);
CREATE INDEX idx_maint_sched_dates ON maintenance_schedule(scheduled_start, scheduled_end);

INSERT INTO app_permission (code, description, active) VALUES
    ('VEHICLE_MAINTENANCE_MANAGE', 'Create, reschedule, complete, and cancel vehicle maintenance schedules', TRUE);
