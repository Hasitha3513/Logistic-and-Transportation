CREATE TABLE tenant_membership_role (
    membership_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (membership_id, role_id),
    CONSTRAINT fk_membership_role_membership FOREIGN KEY (membership_id)
        REFERENCES tenant_membership(membership_id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_role_role FOREIGN KEY (role_id)
        REFERENCES app_role(id) ON DELETE CASCADE
);

INSERT INTO tenant_membership_role (membership_id, role_id)
SELECT membership.membership_id, assignment.role_id
FROM app_user_role assignment
JOIN tenant_membership membership ON membership.user_id = assignment.user_id;

ALTER TABLE refresh_token ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE customer ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE department ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE location ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE project ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vendor ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver_license ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver_exception ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver_violation ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver_medical_record ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE driver_drug_test ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle_category ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle_type ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle_document ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle_reading ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE vehicle_meter_reset ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE maintenance_schedule ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE lubricant_log ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE route ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE route_stop ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE route_revision ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE route_revision_stop ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE route_disruption ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE trip ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE trip_status_history ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE trip_dispatch ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE trip_operational_event ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_station ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_limit_policy ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_issue ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_issue_history ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_price ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_purchase ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE fuel_purchase_history ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE bunker_tank ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE bunker_stock_movement ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE bunker_dip_reading ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE bunker_stock_adjustment ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE freight_order ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE freight_order_line ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE cargo_manifest ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE cargo_manifest_item ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE load_plan ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE load_plan_item_placement ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE freight_insurance_policy ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE freight_insurance_claim ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE freight_insurance_settlement ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE cargo_exception ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE cargo_exception_history ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification_rule ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification_rule_policy ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification_rule_quiet_day ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification_rule_execution ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE notification_delivery_attempt ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;
ALTER TABLE offline_sync_operation ADD COLUMN tenant_id UUID DEFAULT '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a' NOT NULL;

CREATE INDEX idx_refresh_token_tenant ON refresh_token(tenant_id);
CREATE INDEX idx_customer_tenant ON customer(tenant_id);
CREATE INDEX idx_department_tenant ON department(tenant_id);
CREATE INDEX idx_location_tenant ON location(tenant_id);
CREATE INDEX idx_project_tenant ON project(tenant_id);
CREATE INDEX idx_vendor_tenant ON vendor(tenant_id);
CREATE INDEX idx_driver_tenant ON driver(tenant_id);
CREATE INDEX idx_driver_license_tenant ON driver_license(tenant_id);
CREATE INDEX idx_driver_exception_tenant ON driver_exception(tenant_id);
CREATE INDEX idx_driver_violation_tenant ON driver_violation(tenant_id);
CREATE INDEX idx_driver_medical_tenant ON driver_medical_record(tenant_id);
CREATE INDEX idx_driver_drug_test_tenant ON driver_drug_test(tenant_id);
CREATE INDEX idx_vehicle_category_tenant ON vehicle_category(tenant_id);
CREATE INDEX idx_vehicle_type_tenant ON vehicle_type(tenant_id);
CREATE INDEX idx_vehicle_tenant ON vehicle(tenant_id);
CREATE INDEX idx_vehicle_document_tenant ON vehicle_document(tenant_id);
CREATE INDEX idx_vehicle_reading_tenant ON vehicle_reading(tenant_id);
CREATE INDEX idx_vehicle_meter_reset_tenant ON vehicle_meter_reset(tenant_id);
CREATE INDEX idx_maintenance_schedule_tenant ON maintenance_schedule(tenant_id);
CREATE INDEX idx_lubricant_log_tenant ON lubricant_log(tenant_id);
CREATE INDEX idx_route_tenant ON route(tenant_id);
CREATE INDEX idx_route_stop_tenant ON route_stop(tenant_id);
CREATE INDEX idx_route_revision_tenant ON route_revision(tenant_id);
CREATE INDEX idx_route_revision_stop_tenant ON route_revision_stop(tenant_id);
CREATE INDEX idx_route_disruption_tenant ON route_disruption(tenant_id);
CREATE INDEX idx_trip_tenant ON trip(tenant_id);
CREATE INDEX idx_trip_history_tenant ON trip_status_history(tenant_id);
CREATE INDEX idx_trip_dispatch_tenant ON trip_dispatch(tenant_id);
CREATE INDEX idx_trip_event_tenant ON trip_operational_event(tenant_id);
CREATE INDEX idx_fuel_station_tenant ON fuel_station(tenant_id);
CREATE INDEX idx_fuel_limit_tenant ON fuel_limit_policy(tenant_id);
CREATE INDEX idx_fuel_issue_tenant ON fuel_issue(tenant_id);
CREATE INDEX idx_fuel_issue_history_tenant ON fuel_issue_history(tenant_id);
CREATE INDEX idx_fuel_price_tenant ON fuel_price(tenant_id);
CREATE INDEX idx_fuel_purchase_tenant ON fuel_purchase(tenant_id);
CREATE INDEX idx_fuel_purchase_history_tenant ON fuel_purchase_history(tenant_id);
CREATE INDEX idx_bunker_tank_tenant ON bunker_tank(tenant_id);
CREATE INDEX idx_bunker_movement_tenant ON bunker_stock_movement(tenant_id);
CREATE INDEX idx_bunker_dip_tenant ON bunker_dip_reading(tenant_id);
CREATE INDEX idx_bunker_adjustment_tenant ON bunker_stock_adjustment(tenant_id);
CREATE INDEX idx_freight_order_tenant ON freight_order(tenant_id);
CREATE INDEX idx_freight_order_line_tenant ON freight_order_line(tenant_id);
CREATE INDEX idx_cargo_manifest_tenant ON cargo_manifest(tenant_id);
CREATE INDEX idx_cargo_manifest_item_tenant ON cargo_manifest_item(tenant_id);
CREATE INDEX idx_load_plan_tenant ON load_plan(tenant_id);
CREATE INDEX idx_load_plan_placement_tenant ON load_plan_item_placement(tenant_id);
CREATE INDEX idx_insurance_policy_tenant ON freight_insurance_policy(tenant_id);
CREATE INDEX idx_insurance_claim_tenant ON freight_insurance_claim(tenant_id);
CREATE INDEX idx_insurance_settlement_tenant ON freight_insurance_settlement(tenant_id);
CREATE INDEX idx_cargo_exception_tenant ON cargo_exception(tenant_id);
CREATE INDEX idx_cargo_exception_history_tenant ON cargo_exception_history(tenant_id);
CREATE INDEX idx_notification_rule_tenant ON notification_rule(tenant_id);
CREATE INDEX idx_notification_tenant ON notification(tenant_id);
CREATE INDEX idx_notification_rule_policy_tenant ON notification_rule_policy(tenant_id);
CREATE INDEX idx_notification_quiet_day_tenant ON notification_rule_quiet_day(tenant_id);
CREATE INDEX idx_notification_execution_tenant ON notification_rule_execution(tenant_id);
CREATE INDEX idx_notification_delivery_tenant ON notification_delivery_attempt(tenant_id);
CREATE INDEX idx_offline_sync_tenant ON offline_sync_operation(tenant_id);
