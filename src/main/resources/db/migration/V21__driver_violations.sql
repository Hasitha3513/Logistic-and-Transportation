-- V21__driver_violations.sql
-- Table for driver traffic violations, penalties, fines, and payment tracking

CREATE TABLE driver_violation (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    trip_id UUID,
    violation_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    violation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    penalty_points INTEGER NOT NULL DEFAULT 0,
    fine_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    paid_at TIMESTAMP WITH TIME ZONE,
    payment_reference VARCHAR(128),
    location VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    CONSTRAINT fk_driver_violation_driver FOREIGN KEY (driver_id) REFERENCES driver (id),
    CONSTRAINT chk_violation_penalty_points CHECK (penalty_points >= 0),
    CONSTRAINT chk_violation_fine_amount CHECK (fine_amount >= 0)
);

CREATE INDEX idx_driver_violation_driver ON driver_violation (driver_id, violation_date DESC);
CREATE INDEX idx_driver_violation_payment ON driver_violation (driver_id, payment_status);

-- Seed DRIVER_VIOLATION_MANAGE permission
INSERT INTO app_permission (code, description, active) VALUES
    ('DRIVER_VIOLATION_MANAGE', 'Record, update, settle, and waive driver traffic violations', TRUE);
