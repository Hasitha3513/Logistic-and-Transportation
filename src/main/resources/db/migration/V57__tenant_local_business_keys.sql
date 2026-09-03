-- P0-04: business identifiers belong to a Tenant. Authentication and role-template
-- identifiers intentionally remain global and are not changed here.

ALTER TABLE customer DROP CONSTRAINT customer_code_key;
ALTER TABLE customer ADD CONSTRAINT uq_customer_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE department DROP CONSTRAINT department_code_key;
ALTER TABLE department ADD CONSTRAINT uq_department_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE location DROP CONSTRAINT location_code_key;
ALTER TABLE location ADD CONSTRAINT uq_location_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE project DROP CONSTRAINT project_code_key;
ALTER TABLE project ADD CONSTRAINT uq_project_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE vendor DROP CONSTRAINT vendor_code_key;
ALTER TABLE vendor ADD CONSTRAINT uq_vendor_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE driver DROP CONSTRAINT driver_employee_number_key;
ALTER TABLE driver ADD CONSTRAINT uq_driver_tenant_employee_number UNIQUE (tenant_id, employee_number);

ALTER TABLE driver_license DROP CONSTRAINT driver_license_license_number_key;
ALTER TABLE driver_license ADD CONSTRAINT uq_driver_license_tenant_number UNIQUE (tenant_id, license_number);

ALTER TABLE vehicle_category DROP CONSTRAINT vehicle_category_code_key;
ALTER TABLE vehicle_category ADD CONSTRAINT uq_vehicle_category_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE vehicle_type DROP CONSTRAINT vehicle_type_code_key;
ALTER TABLE vehicle_type ADD CONSTRAINT uq_vehicle_type_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE vehicle DROP CONSTRAINT vehicle_registration_number_key;
ALTER TABLE vehicle ADD CONSTRAINT uq_vehicle_tenant_registration UNIQUE (tenant_id, registration_number);

ALTER TABLE route DROP CONSTRAINT route_code_key;
ALTER TABLE route ADD CONSTRAINT uq_route_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE trip DROP CONSTRAINT trip_trip_number_key;
ALTER TABLE trip ADD CONSTRAINT uq_trip_tenant_number UNIQUE (tenant_id, trip_number);

ALTER TABLE fuel_station DROP CONSTRAINT fuel_station_code_key;
ALTER TABLE fuel_station ADD CONSTRAINT uq_fuel_station_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE fuel_issue DROP CONSTRAINT fuel_issue_voucher_number_key;
ALTER TABLE fuel_issue ADD CONSTRAINT uq_fuel_issue_tenant_voucher UNIQUE (tenant_id, voucher_number);

ALTER TABLE fuel_purchase DROP CONSTRAINT fuel_purchase_purchase_number_key;
ALTER TABLE fuel_purchase ADD CONSTRAINT uq_fuel_purchase_tenant_number UNIQUE (tenant_id, purchase_number);

ALTER TABLE fuel_purchase DROP CONSTRAINT uq_fuel_purchase_vendor_invoice;
ALTER TABLE fuel_purchase ADD CONSTRAINT uq_fuel_purchase_tenant_vendor_invoice
    UNIQUE (tenant_id, vendor_id, invoice_number);

ALTER TABLE bunker_tank DROP CONSTRAINT uq_bunker_tank_code;
ALTER TABLE bunker_tank ADD CONSTRAINT uq_bunker_tank_tenant_code UNIQUE (tenant_id, tank_code);

ALTER TABLE freight_order DROP CONSTRAINT freight_order_order_number_key;
ALTER TABLE freight_order ADD CONSTRAINT uq_freight_order_tenant_number UNIQUE (tenant_id, order_number);

ALTER TABLE cargo_manifest DROP CONSTRAINT cargo_manifest_manifest_number_key;
ALTER TABLE cargo_manifest ADD CONSTRAINT uq_cargo_manifest_tenant_number UNIQUE (tenant_id, manifest_number);

ALTER TABLE load_plan DROP CONSTRAINT load_plan_load_plan_number_key;
ALTER TABLE load_plan ADD CONSTRAINT uq_load_plan_tenant_number UNIQUE (tenant_id, load_plan_number);

ALTER TABLE freight_insurance_policy DROP CONSTRAINT freight_insurance_policy_policy_number_key;
ALTER TABLE freight_insurance_policy ADD CONSTRAINT uq_freight_policy_tenant_number UNIQUE (tenant_id, policy_number);

ALTER TABLE freight_insurance_claim DROP CONSTRAINT freight_insurance_claim_claim_number_key;
ALTER TABLE freight_insurance_claim ADD CONSTRAINT uq_freight_claim_tenant_number UNIQUE (tenant_id, claim_number);

ALTER TABLE cargo_exception DROP CONSTRAINT cargo_exception_exception_number_key;
ALTER TABLE cargo_exception ADD CONSTRAINT uq_cargo_exception_tenant_number UNIQUE (tenant_id, exception_number);

-- Idempotency and execution keys are tenant-owned even when their source value is reused.
DROP INDEX uq_bunker_movement_idempotency;
CREATE UNIQUE INDEX uq_bunker_movement_tenant_idempotency
    ON bunker_stock_movement(tenant_id, tank_id, movement_type, reference_type, reference_id);

ALTER TABLE notification_rule_execution DROP CONSTRAINT uq_notif_execution_key;
ALTER TABLE notification_rule_execution ADD CONSTRAINT uq_notif_execution_tenant_key
    UNIQUE (tenant_id, execution_key);
