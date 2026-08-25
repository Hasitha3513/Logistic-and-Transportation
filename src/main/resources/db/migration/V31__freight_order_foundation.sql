CREATE SEQUENCE freight_order_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE freight_order (
    id UUID PRIMARY KEY,
    order_number VARCHAR(60) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    origin_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    requested_pickup_at TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_delivery_at TIMESTAMP WITH TIME ZONE NOT NULL,
    service_level VARCHAR(60) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    special_handling_instructions VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT fk_freight_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_freight_order_origin FOREIGN KEY (origin_location_id) REFERENCES location(id),
    CONSTRAINT fk_freight_order_destination FOREIGN KEY (destination_location_id) REFERENCES location(id),
    CONSTRAINT chk_freight_order_locations CHECK (origin_location_id <> destination_location_id),
    CONSTRAINT chk_freight_order_window CHECK (requested_delivery_at >= requested_pickup_at)
);

CREATE TABLE freight_order_line (
    id UUID PRIMARY KEY,
    freight_order_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    line_order INTEGER NOT NULL,
    CONSTRAINT fk_freight_order_line_order FOREIGN KEY (freight_order_id) REFERENCES freight_order(id) ON DELETE CASCADE,
    CONSTRAINT uq_freight_order_line_order UNIQUE (freight_order_id, line_order),
    CONSTRAINT chk_freight_order_line_quantity CHECK (quantity > 0),
    CONSTRAINT chk_freight_order_line_position CHECK (line_order >= 0)
);

CREATE INDEX idx_freight_order_customer ON freight_order(customer_id);
CREATE INDEX idx_freight_order_pickup ON freight_order(requested_pickup_at);
CREATE INDEX idx_freight_order_line_parent ON freight_order_line(freight_order_id);

INSERT INTO app_permission (code, description, active) VALUES
    ('FREIGHT_ORDER_VIEW', 'View freight orders', TRUE),
    ('FREIGHT_ORDER_MANAGE', 'Create and update freight orders', TRUE);
