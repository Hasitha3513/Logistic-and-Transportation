CREATE SEQUENCE fuel_purchase_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE vendor (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    contact_person VARCHAR(160),
    phone VARCHAR(40),
    email VARCHAR(160),
    active BOOLEAN NOT NULL
);

CREATE TABLE fuel_price (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    fuel_type VARCHAR(40) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    unit_price NUMERIC(19,4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fuel_price_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
    CONSTRAINT chk_fuel_price_positive CHECK (unit_price > 0),
    CONSTRAINT chk_fuel_price_period CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE TABLE fuel_purchase (
    id UUID PRIMARY KEY,
    purchase_number VARCHAR(60) NOT NULL UNIQUE,
    vendor_id UUID NOT NULL,
    fuel_station_id UUID,
    fuel_type VARCHAR(40) NOT NULL,
    purchase_date DATE NOT NULL,
    invoice_number VARCHAR(100),
    invoice_date DATE,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    tax_rate NUMERIC(8,4) NOT NULL,
    tax_amount NUMERIC(19,2) NOT NULL,
    other_charges NUMERIC(19,2) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reconciliation_status VARCHAR(30) NOT NULL,
    received_quantity NUMERIC(19,4),
    quantity_variance NUMERIC(19,4),
    expected_unit_price NUMERIC(19,4),
    price_variance NUMERIC(19,2),
    destination_fuel_station_id UUID,
    delivery_note_number VARCHAR(100),
    received_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    reconciled_by UUID,
    reconciled_at TIMESTAMP WITH TIME ZONE,
    reconciliation_notes VARCHAR(1000),
    reconciliation_reference VARCHAR(100),
    notes VARCHAR(1000),
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_fuel_purchase_vendor_invoice UNIQUE (vendor_id, invoice_number),
    CONSTRAINT fk_fuel_purchase_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
    CONSTRAINT fk_fuel_purchase_station FOREIGN KEY (fuel_station_id) REFERENCES fuel_station(id),
    CONSTRAINT fk_fuel_purchase_destination FOREIGN KEY (destination_fuel_station_id) REFERENCES fuel_station(id),
    CONSTRAINT fk_fuel_purchase_approved_by FOREIGN KEY (approved_by) REFERENCES app_user(id),
    CONSTRAINT fk_fuel_purchase_reconciled_by FOREIGN KEY (reconciled_by) REFERENCES app_user(id),
    CONSTRAINT fk_fuel_purchase_created_by FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT chk_fuel_purchase_quantity CHECK (quantity > 0),
    CONSTRAINT chk_fuel_purchase_price CHECK (unit_price > 0),
    CONSTRAINT chk_fuel_purchase_tax CHECK (tax_rate >= 0 AND tax_amount >= 0),
    CONSTRAINT chk_fuel_purchase_charges CHECK (other_charges >= 0),
    CONSTRAINT chk_fuel_purchase_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','RECEIVED','RECONCILED','CANCELLED')),
    CONSTRAINT chk_fuel_reconciliation_status CHECK (reconciliation_status IN ('PENDING','RECONCILED','NOT_APPLICABLE'))
);

CREATE TABLE fuel_purchase_history (
    id UUID PRIMARY KEY,
    fuel_purchase_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_id UUID NOT NULL,
    actor VARCHAR(80) NOT NULL,
    comment VARCHAR(1000),
    quantity_variance NUMERIC(19,4),
    price_variance NUMERIC(19,2),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fuel_purchase_history_purchase FOREIGN KEY (fuel_purchase_id) REFERENCES fuel_purchase(id),
    CONSTRAINT fk_fuel_purchase_history_actor FOREIGN KEY (actor_id) REFERENCES app_user(id)
);

CREATE INDEX idx_vendor_active ON vendor(active, name);
CREATE INDEX idx_fuel_price_lookup ON fuel_price(vendor_id, fuel_type, active, effective_from, effective_to);
CREATE INDEX idx_fuel_purchase_vendor ON fuel_purchase(vendor_id);
CREATE INDEX idx_fuel_purchase_invoice ON fuel_purchase(invoice_number);
CREATE INDEX idx_fuel_purchase_status ON fuel_purchase(status, reconciliation_status);
CREATE INDEX idx_fuel_purchase_date ON fuel_purchase(purchase_date);
CREATE INDEX idx_fuel_purchase_type ON fuel_purchase(fuel_type);
CREATE INDEX idx_fuel_purchase_history ON fuel_purchase_history(fuel_purchase_id, occurred_at);

INSERT INTO app_permission (code, description, active) VALUES
    ('FUEL_PURCHASE_VIEW', 'View fuel purchases and purchase history', TRUE),
    ('FUEL_PURCHASE_CREATE', 'Create fuel purchases', TRUE),
    ('FUEL_PURCHASE_UPDATE', 'Update draft fuel purchases', TRUE),
    ('FUEL_PURCHASE_SUBMIT', 'Submit fuel purchases for approval', TRUE),
    ('FUEL_PURCHASE_APPROVE', 'Approve submitted fuel purchases', TRUE),
    ('FUEL_PURCHASE_RECEIVE', 'Record receipt of approved fuel purchases', TRUE),
    ('FUEL_PURCHASE_RECONCILE', 'Reconcile received fuel purchases', TRUE),
    ('FUEL_PURCHASE_CANCEL', 'Cancel eligible fuel purchases', TRUE),
    ('FUEL_PRICE_VIEW', 'View fuel vendors and price catalogue', TRUE),
    ('FUEL_PRICE_MANAGE', 'Maintain fuel vendors and price catalogue', TRUE);
