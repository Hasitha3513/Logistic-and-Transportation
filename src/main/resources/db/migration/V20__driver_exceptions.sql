-- V20__driver_exceptions.sql
-- Table for driver exceptions, leaves, and availability-blocking time windows

CREATE TABLE driver_exception (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    exception_type VARCHAR(64) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    CONSTRAINT fk_driver_exception_driver FOREIGN KEY (driver_id) REFERENCES driver (id),
    CONSTRAINT chk_driver_exception_dates CHECK (end_time > start_time)
);

CREATE INDEX idx_driver_exception_driver_status ON driver_exception (driver_id, status);
CREATE INDEX idx_driver_exception_times ON driver_exception (start_time, end_time);

-- Seed DRIVER_EXCEPTION_MANAGE permission
INSERT INTO app_permission (code, description, active) VALUES
    ('DRIVER_EXCEPTION_MANAGE', 'Create, update, cancel, and complete driver exceptions and leaves', TRUE);
