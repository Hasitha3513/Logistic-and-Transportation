CREATE SEQUENCE fuel_voucher_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE fuel_station (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    station_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    vendor_id UUID,
    location_id UUID,
    CONSTRAINT chk_fuel_station_type CHECK (station_type IN ('INTERNAL', 'EXTERNAL')),
    CONSTRAINT fk_fuel_station_location FOREIGN KEY (location_id) REFERENCES location(id)
);

CREATE TABLE fuel_limit_policy (
    id UUID PRIMARY KEY,
    vehicle_id UUID,
    maximum_quantity_per_issue NUMERIC(19,3) NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT chk_fuel_limit_positive CHECK (maximum_quantity_per_issue > 0),
    CONSTRAINT fk_fuel_limit_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id)
);

CREATE TABLE fuel_issue (
    id UUID PRIMARY KEY,
    voucher_number VARCHAR(60) NOT NULL UNIQUE,
    vehicle_id UUID NOT NULL,
    trip_id UUID,
    driver_id UUID,
    fuel_type VARCHAR(40) NOT NULL,
    quantity NUMERIC(19,3) NOT NULL,
    unit_price NUMERIC(19,4),
    total_amount NUMERIC(19,2),
    station_id UUID NOT NULL,
    odometer NUMERIC(19,3),
    engine_hours NUMERIC(19,3),
    issue_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_by UUID NOT NULL,
    authorized_by UUID,
    authorization_date_time TIMESTAMP WITH TIME ZONE,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_fuel_issue_quantity CHECK (quantity > 0),
    CONSTRAINT chk_fuel_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT chk_fuel_odometer CHECK (odometer IS NULL OR odometer >= 0),
    CONSTRAINT chk_fuel_engine_hours CHECK (engine_hours IS NULL OR engine_hours >= 0),
    CONSTRAINT chk_fuel_issue_status CHECK (status IN ('DRAFT', 'PENDING_AUTHORIZATION', 'AUTHORIZED', 'ISSUED', 'CANCELLED')),
    CONSTRAINT fk_fuel_issue_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT fk_fuel_issue_trip FOREIGN KEY (trip_id) REFERENCES trip(id),
    CONSTRAINT fk_fuel_issue_driver FOREIGN KEY (driver_id) REFERENCES driver(id),
    CONSTRAINT fk_fuel_issue_station FOREIGN KEY (station_id) REFERENCES fuel_station(id),
    CONSTRAINT fk_fuel_issue_requested_by FOREIGN KEY (requested_by) REFERENCES app_user(id),
    CONSTRAINT fk_fuel_issue_authorized_by FOREIGN KEY (authorized_by) REFERENCES app_user(id)
);

CREATE TABLE fuel_issue_history (
    id UUID PRIMARY KEY,
    fuel_issue_id UUID NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_id UUID NOT NULL,
    actor VARCHAR(80) NOT NULL,
    comment VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fuel_history_issue FOREIGN KEY (fuel_issue_id) REFERENCES fuel_issue(id),
    CONSTRAINT fk_fuel_history_actor FOREIGN KEY (actor_id) REFERENCES app_user(id)
);

CREATE INDEX idx_fuel_issue_vehicle ON fuel_issue(vehicle_id);
CREATE INDEX idx_fuel_issue_trip ON fuel_issue(trip_id);
CREATE INDEX idx_fuel_issue_date ON fuel_issue(issue_date_time);
CREATE INDEX idx_fuel_issue_status ON fuel_issue(status);
CREATE INDEX idx_fuel_issue_history ON fuel_issue_history(fuel_issue_id, occurred_at);
CREATE INDEX idx_fuel_limit_vehicle ON fuel_limit_policy(vehicle_id, active);

INSERT INTO app_permission (code, description, active) VALUES
    ('FUEL_ISSUE_VIEW', 'View fuel issues, stations, and fuel issue history', TRUE),
    ('FUEL_ISSUE_CREATE', 'Create fuel issues and fuel station references', TRUE),
    ('FUEL_ISSUE_UPDATE', 'Update draft fuel issues and fuel station references', TRUE),
    ('FUEL_ISSUE_SUBMIT', 'Submit draft fuel issues for authorization', TRUE),
    ('FUEL_ISSUE_AUTHORIZE', 'Authorize pending fuel issues', TRUE),
    ('FUEL_ISSUE_ISSUE', 'Record authorized fuel as issued', TRUE),
    ('FUEL_ISSUE_CANCEL', 'Cancel eligible fuel issues', TRUE);
