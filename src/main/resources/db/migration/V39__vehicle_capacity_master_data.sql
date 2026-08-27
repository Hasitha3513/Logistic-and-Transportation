ALTER TABLE vehicle ADD COLUMN tare_weight_kg DOUBLE PRECISION;
ALTER TABLE vehicle ADD COLUMN gross_vehicle_weight_kg DOUBLE PRECISION;
ALTER TABLE vehicle ADD COLUMN cargo_volume_capacity_m3 DOUBLE PRECISION;
ALTER TABLE vehicle ADD COLUMN axle_count INT;
ALTER TABLE vehicle ADD COLUMN max_axle_load_kg DOUBLE PRECISION;

ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_tare_weight CHECK (tare_weight_kg IS NULL OR tare_weight_kg >= 0);
ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_gvw CHECK (gross_vehicle_weight_kg IS NULL OR gross_vehicle_weight_kg >= 0);
ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_cargo_volume CHECK (cargo_volume_capacity_m3 IS NULL OR cargo_volume_capacity_m3 >= 0);
ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_axle_count CHECK (axle_count IS NULL OR axle_count > 0);
ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_max_axle_load CHECK (max_axle_load_kg IS NULL OR max_axle_load_kg >= 0);
ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_gvw_gte_tare CHECK (
    gross_vehicle_weight_kg IS NULL OR tare_weight_kg IS NULL OR gross_vehicle_weight_kg >= tare_weight_kg
);
