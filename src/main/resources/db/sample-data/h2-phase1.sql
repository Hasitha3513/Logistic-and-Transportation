-- =================================================================================================
-- TRANSPORT & LOGISTICS COMPREHENSIVE SAMPLE DATA (ALL SCENARIOS)
-- Compatible with PostgreSQL & H2
-- =================================================================================================

-- -------------------------------------------------------------------------------------------------
-- 1. SYSTEM APP USERS
-- -------------------------------------------------------------------------------------------------
INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'system.sample.admin', 'sample.admin@example.com', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'System', 'Admin', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002', 'ops.manager', 'ops.manager@example.com', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'Operations', 'Manager', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000003', 'fuel.officer', 'fuel.officer@example.com', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'Fuel', 'Officer', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- -------------------------------------------------------------------------------------------------
-- 2. MASTER DATA: CUSTOMERS, DEPARTMENTS, PROJECTS, LOCATIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO customer (id, code, name, contact_person, phone, email, active)
VALUES ('10000000-0000-0000-0000-000000000001', 'CUST-ACME', 'Acme Distribution PLC', 'Nadeesha Perera', '+94 11 555 0101', 'operations@acme.example', TRUE);

INSERT INTO customer (id, code, name, contact_person, phone, email, active)
VALUES ('10000000-0000-0000-0000-000000000002', 'CUST-CEYLON', 'Ceylon Retail Network', 'Kamal Silva', '+94 11 555 0102', 'dispatch@ceylon.example', TRUE);

INSERT INTO customer (id, code, name, contact_person, phone, email, active)
VALUES ('10000000-0000-0000-0000-000000000003', 'CUST-APEX', 'Apex Pharma Logistics', 'Dr. Rohana Jayawardena', '+94 11 555 0103', 'supply@apexpharma.example', TRUE);

INSERT INTO customer (id, code, name, contact_person, phone, email, active)
VALUES ('10000000-0000-0000-0000-000000000004', 'CUST-AGRO', 'Lanka Agro Produce Exports', 'Sunil Wickrama', '+94 81 555 0104', 'exports@lankaagro.example', TRUE);

INSERT INTO department (id, code, name, description, active)
VALUES ('11000000-0000-0000-0000-000000000001', 'DEPT-OPS', 'Transport Operations', 'Daily commercial fleet and logistics operations', TRUE);

INSERT INTO department (id, code, name, description, active)
VALUES ('11000000-0000-0000-0000-000000000002', 'DEPT-COLD', 'Cold Chain Logistics', 'Temperature-controlled pharmaceutical and dairy transport', TRUE);

INSERT INTO department (id, code, name, description, active)
VALUES ('11000000-0000-0000-0000-000000000003', 'DEPT-HEAVY', 'Heavy Freight & Bulk Cargo', 'Containerised industrial port and inter-city haulage', TRUE);

INSERT INTO project (id, code, name, department_id, active)
VALUES ('12000000-0000-0000-0000-000000000001', 'PRJ-WEST', 'Western Province Distribution', '11000000-0000-0000-0000-000000000001', TRUE);

INSERT INTO project (id, code, name, department_id, active)
VALUES ('12000000-0000-0000-0000-000000000002', 'PRJ-CENTRAL', 'Central Expressway Freight Corridor', '11000000-0000-0000-0000-000000000001', TRUE);

INSERT INTO project (id, code, name, department_id, active)
VALUES ('12000000-0000-0000-0000-000000000003', 'PRJ-PHARMA', 'National Islandwide Vaccine & Cold Chain', '11000000-0000-0000-0000-000000000002', TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000001', 'LOC-CMB', 'Colombo Central Logistics Hub', '100 Orugodawatta Logistics Park, Colombo 10', 6.9271, 79.8612, TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000002', 'LOC-KDY', 'Kandy Regional Depot', '45 William Gopallawa Mawatha, Kandy', 7.2906, 80.6337, TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000003', 'LOC-GLE', 'Galle Coastal Terminal', '12 Harbour Road, Galle Fort, Galle', 6.0329, 80.2168, TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000004', 'LOC-KGN', 'Kurunegala Transit Station', '88 Dambulla Road, Kurunegala', 7.4863, 80.3647, TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000005', 'LOC-JAF', 'Jaffna Northern Logistics Base', '250 KKS Road, Jaffna', 9.6615, 80.0255, TRUE);

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES ('20000000-0000-0000-0000-000000000006', 'LOC-HBT', 'Hambantota Port Gate Center', 'Port Access Highway, Hambantota', 6.1246, 81.1185, TRUE);

-- -------------------------------------------------------------------------------------------------
-- 3. FLEET: CATEGORIES, TYPES, VEHICLES, DOCUMENTS, READINGS
-- -------------------------------------------------------------------------------------------------
INSERT INTO vehicle_category (id, code, name, description, active)
VALUES ('30000000-0000-0000-0000-000000000001', 'CAT-TRUCK', 'Medium & Heavy Trucks', 'Rigid medium and heavy goods transport vehicles', TRUE);

INSERT INTO vehicle_category (id, code, name, description, active)
VALUES ('30000000-0000-0000-0000-000000000002', 'CAT-VAN', 'Light Delivery Vans', 'Light commercial urban distribution vans', TRUE);

INSERT INTO vehicle_category (id, code, name, description, active)
VALUES ('30000000-0000-0000-0000-000000000003', 'CAT-PRIME', 'Prime Movers & Articulated', 'Heavy articulated tractor units for 40ft containers', TRUE);

INSERT INTO vehicle_category (id, code, name, description, active)
VALUES ('30000000-0000-0000-0000-000000000004', 'CAT-COLD', 'Refrigerated Reefer Fleet', 'Insulated climate-controlled transport vehicles', TRUE);

INSERT INTO vehicle_type (id, category_id, code, name, description, active)
VALUES ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'TYPE-BOX', '6-Wheel Enclosed Box Truck', 'Fully enclosed aluminum body cargo truck', TRUE);

INSERT INTO vehicle_type (id, category_id, code, name, description, active)
VALUES ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'TYPE-VAN', 'High-Roof Panel Delivery Van', 'City courier delivery van with side slider', TRUE);

INSERT INTO vehicle_type (id, category_id, code, name, description, active)
VALUES ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'TYPE-PRIME-40', '40ft Container Prime Mover', '6x4 Heavy haulage prime mover with fifth-wheel', TRUE);

INSERT INTO vehicle_type (id, category_id, code, name, description, active)
VALUES ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'TYPE-REEFER-10T', '10-Ton Climate Controlled Reefer', 'Multi-temperature refrigeration unit (-20C to +15C)', TRUE);

-- Vehicle 1: Available Box Truck
INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES ('32000000-0000-0000-0000-000000000001', 'WP-CAB-1201', 'CHASSIS-ISZ-1201', 'ENG-4HK1-1201', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Isuzu', 'NPR 75', 2023, 'COMPANY_OWNED', 'AVAILABLE', 42500, 2100, 5500, TRUE);

-- Vehicle 2: Available Delivery Van
INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES ('32000000-0000-0000-0000-000000000002', 'WP-CAA-2202', 'CHASSIS-TOY-2202', 'ENG-1GD-2202', '30000000-0000-0000-0000-000000000002', '31000000-0000-0000-0000-000000000002', 'Toyota', 'HiAce Super GL', 2024, 'COMPANY_OWNED', 'AVAILABLE', 18200, 890, 1400, TRUE);

-- Vehicle 3: In Maintenance Heavy Truck (Brake Overhaul)
INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES ('32000000-0000-0000-0000-000000000003', 'CP-CAB-3303', 'CHASSIS-MIT-3303', 'ENG-6D16-3303', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Mitsubishi', 'Fuso Fighter', 2021, 'COMPANY_OWNED', 'MAINTENANCE', 88600, 4300, 6000, TRUE);

-- Vehicle 4: Available Reefer Truck
INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES ('32000000-0000-0000-0000-000000000004', 'WP-CAC-4404', 'CHASSIS-HIN-4404', 'ENG-J05E-4404', '30000000-0000-0000-0000-000000000004', '31000000-0000-0000-0000-000000000004', 'Hino', 'Dutro Reefer 500', 2023, 'COMPANY_OWNED', 'AVAILABLE', 29400, 1450, 3500, TRUE);

-- Vehicle 5: Prime Mover currently ON_TRIP
INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES ('32000000-0000-0000-0000-000000000005', 'WP-CAD-5505', 'CHASSIS-VOL-5505', 'ENG-D16K-5505', '30000000-0000-0000-0000-000000000003', '31000000-0000-0000-0000-000000000003', 'Volvo', 'FH16 750 Globetrotter', 2022, 'LEASED', 'ON_TRIP', 115000, 5600, 28000, TRUE);

-- Vehicle Documents
INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'INSURANCE', 'INS-WP-1201-2026', CURRENT_DATE - 30, CURRENT_DATE + 335, 'https://docs.local/vehicles/wp-cab-1201/insurance.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000001', 'REVENUE_LICENSE', 'REV-WP-1201-2026', CURRENT_DATE - 60, CURRENT_DATE + 305, 'https://docs.local/vehicles/wp-cab-1201/revenue.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000002', 'INSURANCE', 'INS-WP-2202-2026', CURRENT_DATE - 45, CURRENT_DATE + 320, 'https://docs.local/vehicles/wp-caa-2202/insurance.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000004', '32000000-0000-0000-0000-000000000003', 'INSURANCE', 'INS-CP-3303-EXPIRED', CURRENT_DATE - 380, CURRENT_DATE - 15, 'https://docs.local/vehicles/cp-cab-3303/insurance-old.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000005', '32000000-0000-0000-0000-000000000004', 'FITNESS_CERTIFICATE', 'FIT-WP-4404-2026', CURRENT_DATE - 100, CURRENT_DATE + 265, 'https://docs.local/vehicles/wp-cac-4404/fitness.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('33000000-0000-0000-0000-000000000006', '32000000-0000-0000-0000-000000000005', 'INSURANCE', 'INS-WP-5505-2026', CURRENT_DATE - 15, CURRENT_DATE + 350, 'https://docs.local/vehicles/wp-cad-5505/insurance.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Vehicle Readings
INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, meter_epoch, source_type, source_reference_id, recorded_at, received_at, created_by, correction_of_reading_id, correction_reason, idempotency_key, notes, created_at)
VALUES ('34000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'ODOMETER', 42000.000, 'KILOMETER', 0, 'BASELINE', '32000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '30' DAY, CURRENT_TIMESTAMP - INTERVAL '30' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-001', 'Fleet onboarding baseline odometer reading', CURRENT_TIMESTAMP);

INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, meter_epoch, source_type, source_reference_id, recorded_at, received_at, created_by, correction_of_reading_id, correction_reason, idempotency_key, notes, created_at)
VALUES ('34000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000001', 'ENGINE_HOURS', 2050.000, 'HOUR', 0, 'BASELINE', '32000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '30' DAY, CURRENT_TIMESTAMP - INTERVAL '30' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-002', 'Fleet onboarding baseline engine hours reading', CURRENT_TIMESTAMP);

INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, meter_epoch, source_type, source_reference_id, recorded_at, received_at, created_by, correction_of_reading_id, correction_reason, idempotency_key, notes, created_at)
VALUES ('34000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000002', 'ODOMETER', 18000.000, 'KILOMETER', 0, 'BASELINE', '32000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '20' DAY, CURRENT_TIMESTAMP - INTERVAL '20' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-003', 'Toyota HiAce baseline reading', CURRENT_TIMESTAMP);

INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, meter_epoch, source_type, source_reference_id, recorded_at, received_at, created_by, correction_of_reading_id, correction_reason, idempotency_key, notes, created_at)
VALUES ('34000000-0000-0000-0000-000000000004', '32000000-0000-0000-0000-000000000005', 'ODOMETER', 114500.000, 'KILOMETER', 0, 'BASELINE', '32000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP - INTERVAL '10' DAY, CURRENT_TIMESTAMP - INTERVAL '10' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-004', 'Volvo Prime Mover baseline reading', CURRENT_TIMESTAMP);

-- Maintenance Schedules
INSERT INTO maintenance_schedule (id, vehicle_id, maintenance_type, scheduled_start, scheduled_end, status, description, service_provider, cost, created_at, updated_at, created_by, updated_by)
VALUES ('35000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'PREVENTIVE_SERVICE', CURRENT_TIMESTAMP - INTERVAL '15' DAY, CURRENT_TIMESTAMP - INTERVAL '14' DAY, 'COMPLETED', '40,000 km Scheduled Engine Service & Filter Change', 'Sathosa Motors Workshop', 45000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO maintenance_schedule (id, vehicle_id, maintenance_type, scheduled_start, scheduled_end, status, description, service_provider, cost, created_at, updated_at, created_by, updated_by)
VALUES ('35000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000003', 'CORRECTIVE_REPAIR', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'IN_PROGRESS', 'Pneumatic Brake Caliper Overhaul & Rotor Replacement', 'United Motors Workshop', 125000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO maintenance_schedule (id, vehicle_id, maintenance_type, scheduled_start, scheduled_end, status, description, service_provider, cost, created_at, updated_at, created_by, updated_by)
VALUES ('35000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000004', 'PERIODIC_INSPECTION', CURRENT_TIMESTAMP + INTERVAL '10' DAY, CURRENT_TIMESTAMP + INTERVAL '11' DAY, 'SCHEDULED', 'Carrier Refrigeration Compressor Gas Leak Check & Calibration', 'ThermoKing Lanka', 35000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Lubricant Consumption Logs
INSERT INTO lubricant_log (id, vehicle_id, fluid_type, quantity, unit, recorded_at, odometer_km, engine_hours, vendor_id, supplier_name, reference_number, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('36000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'ENGINE_OIL', 12.50, 'LITRE', CURRENT_TIMESTAMP - INTERVAL '15' DAY, 42000.00, 2050.00, NULL, 'Caltex Lanka Depot', 'LUB-REF-001', 'Engine oil top-up during scheduled PM service', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO lubricant_log (id, vehicle_id, fluid_type, quantity, unit, recorded_at, odometer_km, engine_hours, vendor_id, supplier_name, reference_number, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('36000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000002', 'COOLANT', 4.00, 'LITRE', CURRENT_TIMESTAMP - INTERVAL '10' DAY, 18100.00, 880.00, NULL, 'Toyota Lanka Workshop', 'LUB-REF-002', 'Radiator coolant flush and refill', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO lubricant_log (id, vehicle_id, fluid_type, quantity, unit, recorded_at, odometer_km, engine_hours, vendor_id, supplier_name, reference_number, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('36000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000005', 'TRANSMISSION_OIL', 18.00, 'LITRE', CURRENT_TIMESTAMP - INTERVAL '5' DAY, 114800.00, 5580.00, NULL, 'Volvo Heavy Duty Service', 'LUB-REF-003', 'I-Shift transmission synthetic gear oil service', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- -------------------------------------------------------------------------------------------------
-- 4. DRIVERS, LICENSES, MEDICALS, DRUG TESTS, VIOLATIONS, EXCEPTIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES ('40000000-0000-0000-0000-000000000001', 'DRV-001', 'Kasun', 'Fernando', '+94 77 555 1001', 'kasun.fernando@example.test', 'AVAILABLE', TRUE);

INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES ('40000000-0000-0000-0000-000000000002', 'DRV-002', 'Amara', 'Jayasinghe', '+94 77 555 1002', 'amara.jayasinghe@example.test', 'AVAILABLE', TRUE);

INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES ('40000000-0000-0000-0000-000000000003', 'DRV-003', 'Ruwan', 'Bandara', '+94 77 555 1003', 'ruwan.bandara@example.test', 'AVAILABLE', TRUE);

INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES ('40000000-0000-0000-0000-000000000004', 'DRV-004', 'Dinesh', 'Perera', '+94 77 555 1004', 'dinesh.perera@example.test', 'UNAVAILABLE', TRUE);

INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES ('40000000-0000-0000-0000-000000000005', 'DRV-005', 'Nimal', 'Siriwardena', '+94 77 555 1005', 'nimal.siriwardena@example.test', 'SUSPENDED', TRUE);

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'B1234567', 'HEAVY', CURRENT_DATE - 730, CURRENT_DATE + 365, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'B7654321', 'LIGHT', CURRENT_DATE - 365, CURRENT_DATE + 730, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('41000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', 'B4567890', 'HEAVY', CURRENT_DATE - 500, CURRENT_DATE + 600, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('41000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000004', 'B8888888', 'HEAVY', CURRENT_DATE - 600, CURRENT_DATE + 400, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES ('41000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000005', 'B5555555', 'HEAVY', CURRENT_DATE - 800, CURRENT_DATE - 10, 'INACTIVE', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Driver Medical Records
INSERT INTO driver_medical_record (id, driver_id, assessment_date, valid_from, valid_until, fitness_status, vision_test_status, restrictions, examiner_or_provider, certificate_reference, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('42000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', CURRENT_DATE - 60, CURRENT_DATE - 60, CURRENT_DATE + 305, 'FIT', 'PASSED', 'None', 'National Transport Medical Institute (NTMI)', 'MED-CERT-2026-001', 'Annual commercial driver fitness certified', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_medical_record (id, driver_id, assessment_date, valid_from, valid_until, fitness_status, vision_test_status, restrictions, examiner_or_provider, certificate_reference, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('42000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', CURRENT_DATE - 90, CURRENT_DATE - 90, CURRENT_DATE + 275, 'FIT_WITH_RESTRICTIONS', 'PASSED_WITH_GLASSES', 'Corrective eye lenses required while driving', 'Asiri Health Diagnostics', 'MED-CERT-2026-002', 'Vision 6/6 with corrective lenses', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_medical_record (id, driver_id, assessment_date, valid_from, valid_until, fitness_status, vision_test_status, restrictions, examiner_or_provider, certificate_reference, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES ('42000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000004', CURRENT_DATE - 15, CURRENT_DATE - 15, CURRENT_DATE + 15, 'TEMPORARILY_UNFIT', 'PENDING', 'Temporary knee injury recovery', 'Nawaloka Medical Hospital', 'MED-CERT-2026-003', 'Physiotherapy underway. Review on completion.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Driver Drug & Alcohol Screening Tests
INSERT INTO driver_drug_test (id, driver_id, test_type, scheduled_date, sample_collected_at, result_date, result, status, laboratory_or_provider, reference_number, remarks, return_to_duty_required, return_to_duty_cleared_at, active, created_at, updated_at, created_by, updated_by)
VALUES ('43000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'RANDOM', CURRENT_DATE - 40, CURRENT_TIMESTAMP - INTERVAL '40' DAY, CURRENT_DATE - 38, 'NEGATIVE', 'COMPLETED', 'Lanka Hospitals Diagnostics', 'DT-REF-2026-001', 'Full panel negative screening', FALSE, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_drug_test (id, driver_id, test_type, scheduled_date, sample_collected_at, result_date, result, status, laboratory_or_provider, reference_number, remarks, return_to_duty_required, return_to_duty_cleared_at, active, created_at, updated_at, created_by, updated_by)
VALUES ('43000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'PRE_EMPLOYMENT', CURRENT_DATE - 200, CURRENT_TIMESTAMP - INTERVAL '200' DAY, CURRENT_DATE - 198, 'NEGATIVE', 'COMPLETED', 'Durdans Laboratory', 'DT-REF-2026-002', 'Pre-employment screening cleared', FALSE, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_drug_test (id, driver_id, test_type, scheduled_date, sample_collected_at, result_date, result, status, laboratory_or_provider, reference_number, remarks, return_to_duty_required, return_to_duty_cleared_at, active, created_at, updated_at, created_by, updated_by)
VALUES ('43000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', 'PERIODIC', CURRENT_DATE + 5, NULL, NULL, 'PENDING', 'SCHEDULED', 'MediHelp Colombo', 'DT-REF-2026-003', 'Scheduled periodic screening', FALSE, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Driver Traffic Violations
INSERT INTO driver_violation (id, driver_id, trip_id, violation_type, severity, violation_date, penalty_points, fine_amount, payment_status, paid_at, payment_reference, location, description, created_at, updated_at, created_by, updated_by)
VALUES ('44000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', NULL, 'SPEEDING', 'MINOR', CURRENT_TIMESTAMP - INTERVAL '60' DAY, 2, 2500.00, 'PAID', CURRENT_TIMESTAMP - INTERVAL '50' DAY, 'POLICE-REC-77881', 'Ambepussa Expressway Exit', 'Speed recorded at 74 km/h in 60 km/h expressway approach', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_violation (id, driver_id, trip_id, violation_type, severity, violation_date, penalty_points, fine_amount, payment_status, paid_at, payment_reference, location, description, created_at, updated_at, created_by, updated_by)
VALUES ('44000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000005', NULL, 'LANE_DISCIPLINE', 'MODERATE', CURRENT_TIMESTAMP - INTERVAL '5' DAY, 3, 3500.00, 'UNPAID', NULL, NULL, 'Peliyagoda Flyover Junction', 'Improper overtaking on heavy vehicle reserved lane', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- Driver Exceptions
INSERT INTO driver_exception (id, driver_id, exception_type, start_time, end_time, status, reason, remarks, created_at, updated_at, created_by, updated_by)
VALUES ('45000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000004', 'MEDICAL_LEAVE', CURRENT_TIMESTAMP - INTERVAL '5' DAY, CURRENT_TIMESTAMP + INTERVAL '10' DAY, 'ACTIVE', 'Knee physiotherapy rehabilitation', 'Approved by Operations Head with medical certificate', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

INSERT INTO driver_exception (id, driver_id, exception_type, start_time, end_time, status, reason, remarks, created_at, updated_at, created_by, updated_by)
VALUES ('45000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'ANNUAL_LEAVE', CURRENT_TIMESTAMP + INTERVAL '20' DAY, CURRENT_TIMESTAMP + INTERVAL '25' DAY, 'APPROVED', 'Scheduled family annual leave', 'Approved roster substitution arranged', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data');

-- -------------------------------------------------------------------------------------------------
-- 5. ROUTES & STOPS
-- -------------------------------------------------------------------------------------------------
INSERT INTO route (id, code, name, origin_location_id, destination_location_id, planned_distance_km, estimated_duration_minutes, active)
VALUES ('50000000-0000-0000-0000-000000000001', 'RTE-CMB-KDY', 'Colombo Central to Kandy Depot', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 116, 240, TRUE);

INSERT INTO route (id, code, name, origin_location_id, destination_location_id, planned_distance_km, estimated_duration_minutes, active)
VALUES ('50000000-0000-0000-0000-000000000002', 'RTE-CMB-GLE', 'Colombo to Galle Southern Expressway', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 125, 150, TRUE);

INSERT INTO route (id, code, name, origin_location_id, destination_location_id, planned_distance_km, estimated_duration_minutes, active)
VALUES ('50000000-0000-0000-0000-000000000003', 'RTE-CMB-JAF', 'Colombo to Jaffna Northern Express Corridor', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 395, 480, TRUE);

INSERT INTO route (id, code, name, origin_location_id, destination_location_id, planned_distance_km, estimated_duration_minutes, active)
VALUES ('50000000-0000-0000-0000-000000000004', 'RTE-CMB-HBT', 'Colombo to Hambantota Port Highway', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000006', 240, 210, TRUE);

INSERT INTO route_stop (route_id, stop_order, location_id)
VALUES ('50000000-0000-0000-0000-000000000001', 0, '20000000-0000-0000-0000-000000000004');

INSERT INTO route_stop (route_id, stop_order, location_id)
VALUES ('50000000-0000-0000-0000-000000000003', 0, '20000000-0000-0000-0000-000000000004');

-- -------------------------------------------------------------------------------------------------
-- 6. TRIPS: COMPLETE LIFECYCLE SCENARIOS
-- -------------------------------------------------------------------------------------------------
-- Scenario 1: DRAFT
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000001', 'TRIP-DEMO-001', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'NORMAL', 'DRAFT', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP + INTERVAL '5' DAY, CURRENT_TIMESTAMP + INTERVAL '5' DAY + INTERVAL '8' HOUR, '31000000-0000-0000-0000-000000000001', 2500, 'Packaged FMCG retail products', 1, 'Call receiver 1 hour before arrival', 'Draft booking created by client portal', NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Scenario 2: SUBMITTED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000002', 'TRIP-DEMO-002', '10000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002', 'HIGH', 'SUBMITTED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP + INTERVAL '6' DAY, CURRENT_TIMESTAMP + INTERVAL '6' DAY + INTERVAL '5' HOUR, '31000000-0000-0000-0000-000000000002', 800, 'High priority retail store stock replenishment', 1, 'Delivery before 10 AM required', 'Submitted for supervisor approval', NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Scenario 3: APPROVED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000003', 'TRIP-DEMO-003', '10000000-0000-0000-0000-000000000003', '11000000-0000-0000-0000-000000000002', '12000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001', 'URGENT', 'APPROVED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP + INTERVAL '7' DAY, CURRENT_TIMESTAMP + INTERVAL '7' DAY + INTERVAL '6' HOUR, '31000000-0000-0000-0000-000000000004', 1800, 'Cold chain pharmaceuticals and insulin vials (2C-8C)', 1, 'Maintain continuous reefer data logger active', 'Approved by Operations Head. Assign reefer vehicle.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Scenario 4: ASSIGNED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000004', 'TRIP-DEMO-004', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'NORMAL', 'ASSIGNED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP + INTERVAL '8' DAY, CURRENT_TIMESTAMP + INTERVAL '8' DAY + INTERVAL '8' HOUR, '31000000-0000-0000-0000-000000000001', 3200, 'Commercial electrical appliances & hardware', 1, 'Handle with care. Use hydraulic lift.', 'Vehicle and driver assigned. Ready for departure gate pass.', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Scenario 5: DISPATCHED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000005', 'TRIP-DEMO-005', '10000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002', 'HIGH', 'DISPATCHED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP + INTERVAL '9' DAY, CURRENT_TIMESTAMP + INTERVAL '9' DAY + INTERVAL '5' HOUR, '31000000-0000-0000-0000-000000000002', 1100, 'High-value consumer goods', 1, 'Express lane transit', 'Dispatched from Colombo Hub. Gate clearance recorded.', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Scenario 6: IN_PROGRESS
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000006', 'TRIP-DEMO-006', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'NORMAL', 'IN_PROGRESS', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP + INTERVAL '4' HOUR, '31000000-0000-0000-0000-000000000001', 3500, 'Consumer goods in active transit', 1, 'Direct delivery to Kandy Warehouse', 'Driver en-route. Checkpoint passed at Kurunegala.', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, NULL, 42500, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP);

-- Scenario 7: COMPLETED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000007', 'TRIP-DEMO-007', '10000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002', 'NORMAL', 'COMPLETED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '6' HOUR, '31000000-0000-0000-0000-000000000002', 750, 'Standard retail store supplies', 1, 'Receipt signed by Galle supervisor', 'Completed on schedule without discrepancies', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR, 18000, 18125, 'Delivered in full and in good condition', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR);

-- Scenario 8: CANCELLED
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000008', 'TRIP-DEMO-008', '10000000-0000-0000-0000-000000000004', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'LOW', 'CANCELLED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '8' HOUR, '31000000-0000-0000-0000-000000000001', 1500, 'Agro fertilizers', 1, 'Hold dispatch pending payment clearance', 'Cancelled per customer written request due to warehouse maintenance', NULL, NULL, NULL, NULL, NULL, NULL, 'Cancelled before allocation', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY);

-- Trip Status Audit History
INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 'DRAFT', 'SUBMITTED', 'TRIP_SUBMITTED', NULL, NULL, NULL, 'sample-data', 'Trip submitted for operational review', CURRENT_TIMESTAMP - INTERVAL '1' DAY);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', 'SUBMITTED', 'APPROVED', 'TRIP_APPROVED', NULL, NULL, NULL, 'sample-data', 'Approved by Operations Head', CURRENT_TIMESTAMP - INTERVAL '18' HOUR);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000004', 'APPROVED', 'APPROVED', 'VEHICLE_ASSIGNED', '32000000-0000-0000-0000-000000000001', NULL, NULL, 'sample-data', 'Vehicle WP-CAB-1201 allocated', CURRENT_TIMESTAMP - INTERVAL '12' HOUR);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000004', 'APPROVED', 'ASSIGNED', 'DRIVER_ASSIGNED', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'HEAVY', 'sample-data', 'Driver Kasun Fernando assigned', CURRENT_TIMESTAMP - INTERVAL '10' HOUR);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000005', 'ASSIGNED', 'DISPATCHED', 'TRIP_DISPATCHED', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'LIGHT', 'sample-data', 'Security gate clearance pass granted', CURRENT_TIMESTAMP - INTERVAL '4' HOUR);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000006', 'DISPATCHED', 'IN_PROGRESS', 'TRIP_STARTED', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'HEAVY', 'sample-data', 'Trip journey started from Colombo Hub', CURRENT_TIMESTAMP - INTERVAL '2' HOUR);

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES ('61000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000007', 'IN_PROGRESS', 'COMPLETED', 'TRIP_COMPLETED', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'LIGHT', 'sample-data', 'Delivered and signed off in Galle', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR);

INSERT INTO trip_dispatch (trip_id, dispatched_at, dispatched_by, remarks)
VALUES ('60000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 'ops.manager', 'Gate pass GP-2026-05 verified');

INSERT INTO trip_dispatch (trip_id, dispatched_at, dispatched_by, remarks)
VALUES ('60000000-0000-0000-0000-000000000006', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'ops.manager', 'Departure inspection clean');

INSERT INTO trip_dispatch (trip_id, dispatched_at, dispatched_by, remarks)
VALUES ('60000000-0000-0000-0000-000000000007', CURRENT_TIMESTAMP - INTERVAL '2' DAY - INTERVAL '1' HOUR, 'ops.manager', 'Dispatched on time');

-- Trip Operational Events: Checkpoints, Delays, Incidents
INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', 'CHECKPOINT', CURRENT_TIMESTAMP - INTERVAL '110' MINUTE, '20000000-0000-0000-0000-000000000001', 'Colombo Depot Exit Gate', 'DEPARTURE_ORIGIN', NULL, NULL, NULL, 'Vehicle left hub on schedule', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000006', 'DELAY', CURRENT_TIMESTAMP - INTERVAL '60' MINUTE, NULL, 'Kadawatha Expressway Interchange', NULL, 30, 'HEAVY_TRAFFIC', NULL, 'Heavy highway congestion due to road construction', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000006', 'CHECKPOINT', CURRENT_TIMESTAMP - INTERVAL '20' MINUTE, '20000000-0000-0000-0000-000000000004', 'Kurunegala Transit Rest Area', 'TRANSIT_WAYPOINT', NULL, NULL, NULL, 'Mandatory 15-min driver rest interval', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000007', 'CHECKPOINT', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '1' HOUR, '20000000-0000-0000-0000-000000000001', 'Colombo Hub', 'DEPARTURE_ORIGIN', NULL, NULL, NULL, 'On-time departure', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000007', 'INCIDENT', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '3' HOUR, NULL, 'Dodangoda Expressway Rest Area', NULL, NULL, 'PUNCTURE', 'LOW', 'Left rear tire slow puncture. Replaced with spare tire in 18 minutes.', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES ('62000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000007', 'CHECKPOINT', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR, '20000000-0000-0000-0000-000000000003', 'Galle Coastal Terminal Gate', 'DESTINATION_ARRIVAL', NULL, NULL, NULL, 'Delivery signed by recipient supervisor', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- -------------------------------------------------------------------------------------------------
-- 7. FUEL & BUNKER MANAGEMENT
-- -------------------------------------------------------------------------------------------------
INSERT INTO vendor (id, code, name, contact_person, phone, email, active)
VALUES ('80000000-0000-0000-0000-000000000001', 'VEND-LIOC', 'Lanka IOC PLC', 'Sunil Wickramasinghe', '+94 11 244 8888', 'corporate@lioc.example', TRUE);

INSERT INTO vendor (id, code, name, contact_person, phone, email, active)
VALUES ('80000000-0000-0000-0000-000000000002', 'VEND-CEYPETCO', 'Ceylon Petroleum Corporation', 'Anura Dissanayake', '+94 11 252 3456', 'sales@ceypetco.example', TRUE);

INSERT INTO vendor (id, code, name, contact_person, phone, email, active)
VALUES ('80000000-0000-0000-0000-000000000003', 'VEND-CALTEX', 'Chevron Lubricants Lanka PLC', 'Pradeep Kumara', '+94 11 452 4520', 'commercial@caltex.example', TRUE);

INSERT INTO fuel_price (id, vendor_id, fuel_type, effective_from, effective_to, unit_price, currency_code, active, created_at, updated_at)
VALUES ('81000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'DIESEL', '2026-01-01', NULL, 310.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO fuel_price (id, vendor_id, fuel_type, effective_from, effective_to, unit_price, currency_code, active, created_at, updated_at)
VALUES ('81000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000001', 'PETROL_92', '2026-01-01', NULL, 365.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO fuel_price (id, vendor_id, fuel_type, effective_from, effective_to, unit_price, currency_code, active, created_at, updated_at)
VALUES ('81000000-0000-0000-0000-000000000003', '80000000-0000-0000-0000-000000000002', 'DIESEL', '2026-01-01', NULL, 305.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO fuel_price (id, vendor_id, fuel_type, effective_from, effective_to, unit_price, currency_code, active, created_at, updated_at)
VALUES ('81000000-0000-0000-0000-000000000004', '80000000-0000-0000-0000-000000000002', 'PETROL_92', '2026-01-01', NULL, 360.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id)
VALUES ('70000000-0000-0000-0000-000000000001', 'FUEL-CMB-INTERNAL', 'Colombo Hub Internal Bunker Station', 'INTERNAL', TRUE, NULL, '20000000-0000-0000-0000-000000000001');

INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id)
VALUES ('70000000-0000-0000-0000-000000000002', 'FUEL-KDY-INTERNAL', 'Kandy Depot Internal Bunker Station', 'INTERNAL', TRUE, NULL, '20000000-0000-0000-0000-000000000002');

INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id)
VALUES ('70000000-0000-0000-0000-000000000003', 'FUEL-IOC-CMB', 'Lanka IOC Colombo Central', 'EXTERNAL', TRUE, '80000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001');

INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id)
VALUES ('70000000-0000-0000-0000-000000000004', 'FUEL-CEY-KDY', 'Ceypetco Kandy Highway Station', 'EXTERNAL', TRUE, '80000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002');

INSERT INTO fuel_limit_policy (id, vehicle_id, maximum_quantity_per_issue, active)
VALUES ('71000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 120.000, TRUE);

INSERT INTO fuel_limit_policy (id, vehicle_id, maximum_quantity_per_issue, active)
VALUES ('71000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000002', 70.000, TRUE);

INSERT INTO fuel_limit_policy (id, vehicle_id, maximum_quantity_per_issue, active)
VALUES ('71000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000004', 100.000, TRUE);

INSERT INTO fuel_limit_policy (id, vehicle_id, maximum_quantity_per_issue, active)
VALUES ('71000000-0000-0000-0000-000000000004', '32000000-0000-0000-0000-000000000005', 450.000, TRUE);

-- Bunker Storage Tanks
INSERT INTO bunker_tank (id, fuel_station_id, tank_code, tank_name, fuel_type, capacity_liters, current_stock_liters, minimum_stock_liters, status, commissioned_at, active, created_at, updated_at)
VALUES ('72000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'BNK-CMB-DSL-01', 'Colombo Hub Main Diesel Tank', 'DIESEL', 10000.000, 6415.000, 1000.000, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60' DAY, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO bunker_tank (id, fuel_station_id, tank_code, tank_name, fuel_type, capacity_liters, current_stock_liters, minimum_stock_liters, status, commissioned_at, active, created_at, updated_at)
VALUES ('72000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', 'BNK-CMB-PET-01', 'Colombo Hub Petrol 92 Tank', 'PETROL_92', 5000.000, 2755.000, 500.000, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60' DAY, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO bunker_tank (id, fuel_station_id, tank_code, tank_name, fuel_type, capacity_liters, current_stock_liters, minimum_stock_liters, status, commissioned_at, active, created_at, updated_at)
VALUES ('72000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000002', 'BNK-KDY-DSL-01', 'Kandy Depot Diesel Tank', 'DIESEL', 8000.000, 4200.000, 800.000, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60' DAY, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Bunker Stock Movement Audit Ledger
INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000001', '72000000-0000-0000-0000-000000000001', 'OPENING_BALANCE', 2500.000, 2500.000, 'INITIAL_SETUP', '72000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '60' DAY, '00000000-0000-0000-0000-000000000001', 'Initial depot tank commissioning', CURRENT_TIMESTAMP);

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000002', '72000000-0000-0000-0000-000000000002', 'OPENING_BALANCE', 1200.000, 1200.000, 'INITIAL_SETUP', '72000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '60' DAY, '00000000-0000-0000-0000-000000000001', 'Initial depot tank commissioning', CURRENT_TIMESTAMP);

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000003', '72000000-0000-0000-0000-000000000001', 'RECEIPT', 4000.000, 6500.000, 'FUEL_PURCHASE', '75000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '15' DAY, '00000000-0000-0000-0000-000000000001', 'Bulk diesel replenishment delivery', CURRENT_TIMESTAMP);

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000004', '72000000-0000-0000-0000-000000000002', 'RECEIPT', 1600.000, 2800.000, 'FUEL_PURCHASE', '75000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '10' DAY, '00000000-0000-0000-0000-000000000001', 'Bulk petrol replenishment delivery', CURRENT_TIMESTAMP);

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000005', '72000000-0000-0000-0000-000000000001', 'DISPENSE', 85.000, 6415.000, 'FUEL_ISSUE', '76000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, '00000000-0000-0000-0000-000000000001', 'Dispensed to vehicle WP-CAB-1201 for Trip TRIP-DEMO-006', CURRENT_TIMESTAMP);

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES ('73000000-0000-0000-0000-000000000006', '72000000-0000-0000-0000-000000000002', 'DISPENSE', 45.000, 2755.000, 'FUEL_ISSUE', '76000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2' DAY, '00000000-0000-0000-0000-000000000001', 'Dispensed to vehicle WP-CAA-2202 for Trip TRIP-DEMO-007', CURRENT_TIMESTAMP);

-- Bunker Physical Dip Readings
INSERT INTO bunker_dip_reading (id, tank_id, physical_quantity_liters, book_quantity_at_measurement, variance_quantity_liters, measured_at, measured_by, notes, created_at)
VALUES ('74000000-0000-0000-0000-000000000001', '72000000-0000-0000-0000-000000000001', 6410.000, 6415.000, -5.000, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, '00000000-0000-0000-0000-000000000001', 'Daily dip reading. Minor normal temperature variance.', CURRENT_TIMESTAMP);

INSERT INTO bunker_dip_reading (id, tank_id, physical_quantity_liters, book_quantity_at_measurement, variance_quantity_liters, measured_at, measured_by, notes, created_at)
VALUES ('74000000-0000-0000-0000-000000000002', '72000000-0000-0000-0000-000000000002', 2755.000, 2755.000, 0.000, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, '00000000-0000-0000-0000-000000000001', 'Exact match with book ledger.', CURRENT_TIMESTAMP);

-- Fuel Purchases
INSERT INTO fuel_purchase (id, purchase_number, vendor_id, fuel_station_id, fuel_type, purchase_date, invoice_number, invoice_date, quantity, unit_price, subtotal, tax_rate, tax_amount, other_charges, total_amount, currency_code, status, reconciliation_status, received_quantity, quantity_variance, expected_unit_price, price_variance, destination_fuel_station_id, delivery_note_number, received_at, approved_by, approved_at, reconciled_by, reconciled_at, reconciliation_notes, reconciliation_reference, notes, created_by, created_at, updated_at)
VALUES ('75000000-0000-0000-0000-000000000001', 'PO-FUEL-2026-001', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'DIESEL', CURRENT_DATE - 15, 'INV-LIOC-8891', CURRENT_DATE - 15, 4000.0000, 310.0000, 1240000.00, 0.0000, 0.00, 0.00, 1240000.00, 'LKR', 'RECONCILED', 'RECONCILED', 4000.0000, 0.0000, 310.0000, 0.00, '70000000-0000-0000-0000-000000000001', 'DN-LIOC-9912', CURRENT_TIMESTAMP - INTERVAL '15' DAY, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '16' DAY, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '14' DAY, 'Invoice fully verified against depot flowmeter', 'REC-REF-001', 'Direct bowser delivery to Colombo Main Diesel Tank', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '17' DAY, CURRENT_TIMESTAMP - INTERVAL '14' DAY);

INSERT INTO fuel_purchase (id, purchase_number, vendor_id, fuel_station_id, fuel_type, purchase_date, invoice_number, invoice_date, quantity, unit_price, subtotal, tax_rate, tax_amount, other_charges, total_amount, currency_code, status, reconciliation_status, received_quantity, quantity_variance, expected_unit_price, price_variance, destination_fuel_station_id, delivery_note_number, received_at, approved_by, approved_at, reconciled_by, reconciled_at, reconciliation_notes, reconciliation_reference, notes, created_by, created_at, updated_at)
VALUES ('75000000-0000-0000-0000-000000000002', 'PO-FUEL-2026-002', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PETROL_92', CURRENT_DATE - 10, 'INV-LIOC-9045', CURRENT_DATE - 10, 1600.0000, 365.0000, 584000.00, 0.0000, 0.00, 0.00, 584000.00, 'LKR', 'RECEIVED', 'PENDING', 1600.0000, 0.0000, 365.0000, 0.00, '70000000-0000-0000-0000-000000000001', 'DN-LIOC-9230', CURRENT_TIMESTAMP - INTERVAL '10' DAY, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '11' DAY, NULL, NULL, NULL, NULL, 'Received into Petrol 92 Tank. Awaiting supplier tax invoice copy.', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '12' DAY, CURRENT_TIMESTAMP - INTERVAL '10' DAY);

INSERT INTO fuel_purchase (id, purchase_number, vendor_id, fuel_station_id, fuel_type, purchase_date, invoice_number, invoice_date, quantity, unit_price, subtotal, tax_rate, tax_amount, other_charges, total_amount, currency_code, status, reconciliation_status, received_quantity, quantity_variance, expected_unit_price, price_variance, destination_fuel_station_id, delivery_note_number, received_at, approved_by, approved_at, reconciled_by, reconciled_at, reconciliation_notes, reconciliation_reference, notes, created_by, created_at, updated_at)
VALUES ('75000000-0000-0000-0000-000000000003', 'PO-FUEL-2026-003', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', 'DIESEL', CURRENT_DATE + 2, NULL, NULL, 3000.0000, 305.0000, 915000.00, 0.0000, 0.00, 0.00, 915000.00, 'LKR', 'APPROVED', 'NOT_APPLICABLE', NULL, NULL, 305.0000, 0.00, '70000000-0000-0000-0000-000000000002', NULL, NULL, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '1' DAY, NULL, NULL, NULL, NULL, 'Approved PO for Kandy Depot replenishment next week.', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY);

INSERT INTO fuel_purchase (id, purchase_number, vendor_id, fuel_station_id, fuel_type, purchase_date, invoice_number, invoice_date, quantity, unit_price, subtotal, tax_rate, tax_amount, other_charges, total_amount, currency_code, status, reconciliation_status, received_quantity, quantity_variance, expected_unit_price, price_variance, destination_fuel_station_id, delivery_note_number, received_at, approved_by, approved_at, reconciled_by, reconciled_at, reconciliation_notes, reconciliation_reference, notes, created_by, created_at, updated_at)
VALUES ('75000000-0000-0000-0000-000000000004', 'PO-FUEL-2026-004', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'DIESEL', CURRENT_DATE + 5, NULL, NULL, 2000.0000, 310.0000, 620000.00, 0.0000, 0.00, 0.00, 620000.00, 'LKR', 'DRAFT', 'NOT_APPLICABLE', NULL, NULL, NULL, NULL, '70000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Draft order for safety stock maintenance', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fuel Issues
INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, driver_id, fuel_type, quantity, unit_price, total_amount, station_id, odometer, engine_hours, issue_date_time, status, requested_by, authorized_by, authorization_date_time, notes, created_at, updated_at)
VALUES ('76000000-0000-0000-0000-000000000001', 'FI-2026-0001', '32000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', 'DIESEL', 85.000, 310.0000, 26350.00, '70000000-0000-0000-0000-000000000001', 42500.000, 2100.000, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'ISSUED', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '3' HOUR, 'Pre-trip tank full top up for Kandy delivery trip TRIP-DEMO-006', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR);

INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, driver_id, fuel_type, quantity, unit_price, total_amount, station_id, odometer, engine_hours, issue_date_time, status, requested_by, authorized_by, authorization_date_time, notes, created_at, updated_at)
VALUES ('76000000-0000-0000-0000-000000000002', 'FI-2026-0002', '32000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000002', 'PETROL_92', 45.000, 365.0000, 16425.00, '70000000-0000-0000-0000-000000000001', 18000.000, 880.000, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'ISSUED', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' DAY - INTERVAL '1' HOUR, 'Southern highway trip fuel voucher for TRIP-DEMO-007', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY);

INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, driver_id, fuel_type, quantity, unit_price, total_amount, station_id, odometer, engine_hours, issue_date_time, status, requested_by, authorized_by, authorization_date_time, notes, created_at, updated_at)
VALUES ('76000000-0000-0000-0000-000000000003', 'FI-2026-0003', '32000000-0000-0000-0000-000000000005', NULL, '40000000-0000-0000-0000-000000000003', 'DIESEL', 350.000, 310.0000, 108500.00, '70000000-0000-0000-0000-000000000001', 115000.000, 5600.000, CURRENT_TIMESTAMP + INTERVAL '1' DAY, 'AUTHORIZED', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'Authorized long-distance bulk fuel voucher for Volvo Prime Mover', CURRENT_TIMESTAMP - INTERVAL '3' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR);

INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, driver_id, fuel_type, quantity, unit_price, total_amount, station_id, odometer, engine_hours, issue_date_time, status, requested_by, authorized_by, authorization_date_time, notes, created_at, updated_at)
VALUES ('76000000-0000-0000-0000-000000000004', 'FI-2026-0004', '32000000-0000-0000-0000-000000000004', NULL, NULL, 'DIESEL', 60.000, 310.0000, 18600.00, '70000000-0000-0000-0000-000000000001', 29400.000, 1450.000, CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'DRAFT', '00000000-0000-0000-0000-000000000001', NULL, NULL, 'Draft fuel requisition for upcoming Reefer delivery', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fuel Issue Audit History
INSERT INTO fuel_issue_history (id, fuel_issue_id, from_status, to_status, action, actor_id, actor, comment, occurred_at)
VALUES ('77000000-0000-0000-0000-000000000001', '76000000-0000-0000-0000-000000000001', 'DRAFT', 'PENDING_AUTHORIZATION', 'SUBMIT', '00000000-0000-0000-0000-000000000001', 'sample.admin', 'Submitted for authorization', CURRENT_TIMESTAMP - INTERVAL '4' HOUR);

INSERT INTO fuel_issue_history (id, fuel_issue_id, from_status, to_status, action, actor_id, actor, comment, occurred_at)
VALUES ('77000000-0000-0000-0000-000000000002', '76000000-0000-0000-0000-000000000001', 'PENDING_AUTHORIZATION', 'AUTHORIZED', 'AUTHORIZE', '00000000-0000-0000-0000-000000000001', 'sample.admin', 'Approved within vehicle quota limits', CURRENT_TIMESTAMP - INTERVAL '3' HOUR);

INSERT INTO fuel_issue_history (id, fuel_issue_id, from_status, to_status, action, actor_id, actor, comment, occurred_at)
VALUES ('77000000-0000-0000-0000-000000000003', '76000000-0000-0000-0000-000000000001', 'AUTHORIZED', 'ISSUED', 'RECORD_ISSUE', '00000000-0000-0000-0000-000000000001', 'sample.admin', '85.0L dispensed at Colombo Bunker Pump #1', CURRENT_TIMESTAMP - INTERVAL '2' HOUR);

-- -------------------------------------------------------------------------------------------------
-- 8. OFFLINE SYNC OPERATIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO offline_sync_operation (operation_id, operation_type, operation_version, actor_id, client_instance_id, aggregate_type, aggregate_id, request_hash, result_status, result_code, result_version, processed_at, created_at)
VALUES ('90000000-0000-0000-0000-000000000001', 'RECORD_CHECKPOINT', 1, '00000000-0000-0000-0000-000000000001', '91000000-0000-0000-0000-000000000001', 'Trip', '60000000-0000-0000-0000-000000000006', 'HASH-SYNC-001', 'APPLIED', 'SUCCESS', 1, CURRENT_TIMESTAMP - INTERVAL '20' MINUTE, CURRENT_TIMESTAMP - INTERVAL '25' MINUTE);
