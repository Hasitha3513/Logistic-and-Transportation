CREATE TABLE bunker_tank (
    id UUID PRIMARY KEY,
    fuel_station_id UUID NOT NULL REFERENCES fuel_station(id),
    tank_code VARCHAR(32) NOT NULL,
    tank_name VARCHAR(128) NOT NULL,
    fuel_type VARCHAR(32) NOT NULL,
    capacity_liters NUMERIC(12, 3) NOT NULL,
    current_stock_liters NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    minimum_stock_liters NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    commissioned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bunker_tank_code UNIQUE (tank_code),
    CONSTRAINT chk_bunker_capacity_positive CHECK (capacity_liters > 0),
    CONSTRAINT chk_bunker_stock_non_negative CHECK (current_stock_liters >= 0),
    CONSTRAINT chk_bunker_stock_capacity CHECK (current_stock_liters <= capacity_liters),
    CONSTRAINT chk_bunker_min_stock_non_negative CHECK (minimum_stock_liters >= 0)
);

CREATE INDEX idx_bunker_tank_station ON bunker_tank(fuel_station_id);
CREATE INDEX idx_bunker_tank_fuel_type ON bunker_tank(fuel_type);

CREATE TABLE bunker_stock_movement (
    id UUID PRIMARY KEY,
    tank_id UUID NOT NULL REFERENCES bunker_tank(id),
    movement_type VARCHAR(32) NOT NULL,
    quantity_liters NUMERIC(12, 3) NOT NULL,
    resulting_balance_liters NUMERIC(12, 3) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL REFERENCES app_user(id),
    reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bunker_movement_qty_positive CHECK (quantity_liters > 0),
    CONSTRAINT chk_bunker_movement_bal_non_negative CHECK (resulting_balance_liters >= 0)
);

CREATE INDEX idx_bunker_movement_tank_time ON bunker_stock_movement(tank_id, occurred_at DESC);
CREATE INDEX idx_bunker_movement_ref ON bunker_stock_movement(reference_type, reference_id);

CREATE UNIQUE INDEX uq_bunker_movement_idempotency
ON bunker_stock_movement(tank_id, movement_type, reference_type, reference_id);

CREATE TABLE bunker_dip_reading (
    id UUID PRIMARY KEY,
    tank_id UUID NOT NULL REFERENCES bunker_tank(id),
    physical_quantity_liters NUMERIC(12, 3) NOT NULL,
    book_quantity_at_measurement NUMERIC(12, 3) NOT NULL,
    variance_quantity_liters NUMERIC(12, 3) NOT NULL,
    measured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    measured_by UUID NOT NULL REFERENCES app_user(id),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bunker_dip_qty_non_negative CHECK (physical_quantity_liters >= 0)
);

CREATE INDEX idx_bunker_dip_tank_time ON bunker_dip_reading(tank_id, measured_at DESC);

CREATE TABLE bunker_stock_adjustment (
    id UUID PRIMARY KEY,
    tank_id UUID NOT NULL REFERENCES bunker_tank(id),
    quantity_delta_liters NUMERIC(12, 3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    approved_by UUID NOT NULL REFERENCES app_user(id),
    source_dip_reading_id UUID REFERENCES bunker_dip_reading(id),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bunker_adjustment_tank ON bunker_stock_adjustment(tank_id, occurred_at DESC);

INSERT INTO app_permission (code, description, active) VALUES
    ('BUNKER_VIEW', 'View bunker tanks, balances, movements and dip readings', TRUE),
    ('BUNKER_CREATE', 'Create and commission new bunker storage tanks', TRUE),
    ('BUNKER_UPDATE', 'Update bunker tank configuration and operational status', TRUE),
    ('BUNKER_LEDGER_VIEW', 'View detailed bunker stock ledger transaction history', TRUE),
    ('BUNKER_DIP_RECORD', 'Record physical dip measurements and calculate variances', TRUE),
    ('BUNKER_ADJUST', 'Initialize opening balances and post stock variance adjustments', TRUE),
    ('BUNKER_TRANSFER', 'Execute fuel transfers between bunker storage tanks', TRUE);
