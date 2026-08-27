ALTER TABLE cargo_manifest_item ADD COLUMN unit_weight DECIMAL(19,4);
ALTER TABLE cargo_manifest_item ADD COLUMN weight_unit VARCHAR(16);
ALTER TABLE cargo_manifest_item ADD COLUMN length DECIMAL(19,4);
ALTER TABLE cargo_manifest_item ADD COLUMN width DECIMAL(19,4);
ALTER TABLE cargo_manifest_item ADD COLUMN height DECIMAL(19,4);
ALTER TABLE cargo_manifest_item ADD COLUMN dimension_unit VARCHAR(16);

ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_unit_weight CHECK (unit_weight IS NULL OR unit_weight > 0);
ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_weight_unit CHECK (weight_unit IS NULL OR weight_unit IN ('KG', 'G', 'TONNE'));
ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_length CHECK (length IS NULL OR length > 0);
ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_width CHECK (width IS NULL OR width > 0);
ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_height CHECK (height IS NULL OR height > 0);
ALTER TABLE cargo_manifest_item ADD CONSTRAINT chk_manifest_item_dim_unit CHECK (dimension_unit IS NULL OR dimension_unit IN ('M', 'CM', 'MM'));
