CREATE SEQUENCE cargo_manifest_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE cargo_manifest (
 id UUID PRIMARY KEY, manifest_number VARCHAR(60) NOT NULL UNIQUE, freight_order_id UUID NOT NULL,
 freight_order_number VARCHAR(60) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 created_by VARCHAR(128) NOT NULL, updated_by VARCHAR(128) NOT NULL,
 finalized_at TIMESTAMP WITH TIME ZONE, finalized_by VARCHAR(128),
 CONSTRAINT fk_cargo_manifest_freight_order FOREIGN KEY (freight_order_id) REFERENCES freight_order(id),
 CONSTRAINT chk_cargo_manifest_finalization CHECK ((finalized_at IS NULL AND finalized_by IS NULL) OR (finalized_at IS NOT NULL AND finalized_by IS NOT NULL))
);
CREATE TABLE cargo_manifest_item (
 id UUID PRIMARY KEY, cargo_manifest_id UUID NOT NULL, freight_order_line_id UUID NOT NULL,
 description VARCHAR(500) NOT NULL, quantity DECIMAL(19,4) NOT NULL, packing_information VARCHAR(500) NOT NULL,
 commodity_classification VARCHAR(120) NOT NULL, customs_applicable BOOLEAN NOT NULL DEFAULT FALSE,
 customs_information VARCHAR(1000), hazardous BOOLEAN NOT NULL DEFAULT FALSE,
 hazardous_classification VARCHAR(120), hazardous_details VARCHAR(1000), item_order INTEGER NOT NULL,
 CONSTRAINT fk_manifest_item_manifest FOREIGN KEY (cargo_manifest_id) REFERENCES cargo_manifest(id) ON DELETE CASCADE,
 CONSTRAINT fk_manifest_item_order_line FOREIGN KEY (freight_order_line_id) REFERENCES freight_order_line(id),
 CONSTRAINT uq_manifest_item_order UNIQUE (cargo_manifest_id,item_order),
 CONSTRAINT chk_manifest_item_quantity CHECK (quantity > 0), CONSTRAINT chk_manifest_item_position CHECK (item_order >= 0)
);
CREATE INDEX idx_cargo_manifest_order ON cargo_manifest(freight_order_id);
CREATE INDEX idx_cargo_manifest_finalized ON cargo_manifest(finalized_at);
CREATE INDEX idx_manifest_item_parent ON cargo_manifest_item(cargo_manifest_id);
INSERT INTO app_permission (code,description,active) VALUES
 ('CARGO_MANIFEST_VIEW','View cargo manifests',TRUE),
 ('CARGO_MANIFEST_MANAGE','Create and update cargo manifests and items',TRUE),
 ('CARGO_MANIFEST_FINALIZE','Finalize cargo manifests',TRUE);
