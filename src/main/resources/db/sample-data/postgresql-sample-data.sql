-- =================================================================================================
-- TRANSPORT & LOGISTICS COMPREHENSIVE POSTGRESQL MOCK DATA
-- Compatible with PostgreSQL 14+ and Flyway Migrations V1 through V38
-- =================================================================================================

-- -------------------------------------------------------------------------------------------------
-- 1. SYSTEM APP USERS & SECURITY
-- -------------------------------------------------------------------------------------------------
INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, phone, active, created_at, updated_at)
VALUES 
  ('00000000-0000-0000-0000-000000000001', 'system.admin', 'admin@transport.local', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'System', 'Administrator', '+94 11 200 0001', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('00000000-0000-0000-0000-000000000002', 'ops.manager', 'ops.manager@transport.local', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'Nimal', 'Jayawardena', '+94 11 200 0002', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('00000000-0000-0000-0000-000000000003', 'fuel.officer', 'fuel.officer@transport.local', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'Sunil', 'Perera', '+94 11 200 0003', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('00000000-0000-0000-0000-000000000004', 'freight.planner', 'freight.planner@transport.local', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'Kavinda', 'Silva', '+94 11 200 0004', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 2. MASTER DATA: CUSTOMERS, DEPARTMENTS, PROJECTS, LOCATIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO customer (id, code, name, contact_person, phone, email, active)
VALUES 
  ('10000000-0000-0000-0000-000000000001', 'CUST-ACME', 'Acme Distribution PLC', 'Nadeesha Perera', '+94 11 555 0101', 'operations@acme.example', TRUE),
  ('10000000-0000-0000-0000-000000000002', 'CUST-CEYLON', 'Ceylon Retail Network', 'Kamal Silva', '+94 11 555 0102', 'dispatch@ceylon.example', TRUE),
  ('10000000-0000-0000-0000-000000000003', 'CUST-APEX', 'Apex Pharma Logistics', 'Dr. Rohana Jayawardena', '+94 11 555 0103', 'supply@apexpharma.example', TRUE),
  ('10000000-0000-0000-0000-000000000004', 'CUST-AGRO', 'Lanka Agro Produce Exports', 'Sunil Wickrama', '+94 81 555 0104', 'exports@lankaagro.example', TRUE),
  ('10000000-0000-0000-0000-000000000005', 'CUST-LANKAFOOD', 'Lanka Fresh Foods Ltd', 'Anoma Senaratne', '+94 11 555 0105', 'logistics@lankafood.example', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO department (id, code, name, description, active)
VALUES 
  ('11000000-0000-0000-0000-000000000001', 'DEPT-OPS', 'Transport Operations', 'Daily commercial fleet and logistics operations', TRUE),
  ('11000000-0000-0000-0000-000000000002', 'DEPT-COLD', 'Cold Chain Logistics', 'Temperature-controlled pharmaceutical and dairy transport', TRUE),
  ('11000000-0000-0000-0000-000000000003', 'DEPT-HEAVY', 'Heavy Freight & Bulk Cargo', 'Containerised industrial port and inter-city haulage', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO project (id, code, name, department_id, active)
VALUES 
  ('12000000-0000-0000-0000-000000000001', 'PRJ-WEST', 'Western Province Distribution', '11000000-0000-0000-0000-000000000001', TRUE),
  ('12000000-0000-0000-0000-000000000002', 'PRJ-CENTRAL', 'Central Expressway Freight Corridor', '11000000-0000-0000-0000-000000000001', TRUE),
  ('12000000-0000-0000-0000-000000000003', 'PRJ-PHARMA', 'National Islandwide Vaccine & Cold Chain', '11000000-0000-0000-0000-000000000002', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO location (id, code, name, address, latitude, longitude, active)
VALUES 
  ('20000000-0000-0000-0000-000000000001', 'LOC-CMB', 'Colombo Central Logistics Hub', '100 Orugodawatta Logistics Park, Colombo 10', 6.9271, 79.8612, TRUE),
  ('20000000-0000-0000-0000-000000000002', 'LOC-KDY', 'Kandy Regional Depot', '45 William Gopallawa Mawatha, Kandy', 7.2906, 80.6337, TRUE),
  ('20000000-0000-0000-0000-000000000003', 'LOC-GLE', 'Galle Coastal Terminal', '12 Harbour Road, Galle Fort, Galle', 6.0329, 80.2168, TRUE),
  ('20000000-0000-0000-0000-000000000004', 'LOC-KGN', 'Kurunegala Transit Station', '88 Dambulla Road, Kurunegala', 7.4863, 80.3647, TRUE),
  ('20000000-0000-0000-0000-000000000005', 'LOC-JAF', 'Jaffna Northern Logistics Base', '250 KKS Road, Jaffna', 9.6615, 80.0255, TRUE),
  ('20000000-0000-0000-0000-000000000006', 'LOC-HBT', 'Hambantota Port Gate Center', 'Port Access Highway, Hambantota', 6.1246, 81.1185, TRUE)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 3. FLEET: CATEGORIES, TYPES, VEHICLES, DOCUMENTS, READINGS, MAINTENANCE & LUBRICANTS
-- -------------------------------------------------------------------------------------------------
INSERT INTO vehicle_category (id, code, name, description, active)
VALUES 
  ('30000000-0000-0000-0000-000000000001', 'CAT-TRUCK', 'Medium & Heavy Trucks', 'Rigid medium and heavy goods transport vehicles', TRUE),
  ('30000000-0000-0000-0000-000000000002', 'CAT-VAN', 'Light Delivery Vans', 'Light commercial urban distribution vans', TRUE),
  ('30000000-0000-0000-0000-000000000003', 'CAT-PRIME', 'Prime Movers & Articulated', 'Heavy articulated tractor units for 40ft containers', TRUE),
  ('30000000-0000-0000-0000-000000000004', 'CAT-COLD', 'Refrigerated Reefer Fleet', 'Insulated climate-controlled transport vehicles', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_type (id, category_id, code, name, description, active)
VALUES 
  ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'TYPE-BOX', '6-Wheel Enclosed Box Truck', 'Fully enclosed aluminum body cargo truck', TRUE),
  ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'TYPE-VAN', 'High-Roof Panel Delivery Van', 'City courier delivery van with side slider', TRUE),
  ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'TYPE-PRIME-40', '40ft Container Prime Mover', '6x4 Heavy haulage prime mover with fifth-wheel', TRUE),
  ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'TYPE-REEFER-10T', '10-Ton Climate Controlled Reefer', 'Multi-temperature refrigeration unit (-20C to +15C)', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active)
VALUES 
  ('32000000-0000-0000-0000-000000000001', 'WP-CAB-1201', 'CHASSIS-ISZ-1201', 'ENG-4HK1-1201', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Isuzu', 'NPR 75', 2023, 'COMPANY_OWNED', 'AVAILABLE', 42500, 2100, 5500, TRUE),
  ('32000000-0000-0000-0000-000000000002', 'WP-CAA-2202', 'CHASSIS-TOY-2202', 'ENG-1GD-2202', '30000000-0000-0000-0000-000000000002', '31000000-0000-0000-0000-000000000002', 'Toyota', 'HiAce Super GL', 2024, 'COMPANY_OWNED', 'AVAILABLE', 18200, 890, 1400, TRUE),
  ('32000000-0000-0000-0000-000000000003', 'CP-CAB-3303', 'CHASSIS-MIT-3303', 'ENG-6D16-3303', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Mitsubishi', 'Fuso Fighter', 2021, 'COMPANY_OWNED', 'MAINTENANCE', 88600, 4300, 6000, TRUE),
  ('32000000-0000-0000-0000-000000000004', 'WP-CAC-4404', 'CHASSIS-HIN-4404', 'ENG-J05E-4404', '30000000-0000-0000-0000-000000000004', '31000000-0000-0000-0000-000000000004', 'Hino', 'Dutro Reefer 500', 2023, 'COMPANY_OWNED', 'AVAILABLE', 29400, 1450, 3500, TRUE),
  ('32000000-0000-0000-0000-000000000005', 'WP-CAD-5505', 'CHASSIS-VOL-5505', 'ENG-D16K-5505', '30000000-0000-0000-0000-000000000003', '31000000-0000-0000-0000-000000000003', 'Volvo', 'FH16 750 Globetrotter', 2022, 'LEASED', 'ON_TRIP', 115000, 5600, 28000, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, issue_date, expiry_date, file_reference, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by)
VALUES 
  ('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'INSURANCE', 'INS-WP-1201-2026', CURRENT_DATE - 30, CURRENT_DATE + 335, 'https://docs.local/vehicles/wp-cab-1201/insurance.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('33000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000001', 'REVENUE_LICENSE', 'REV-WP-1201-2026', CURRENT_DATE - 60, CURRENT_DATE + 305, 'https://docs.local/vehicles/wp-cab-1201/revenue.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('33000000-0000-0000-0000-000000000003', '32000000-0000-0000-0000-000000000002', 'INSURANCE', 'INS-WP-2202-2026', CURRENT_DATE - 45, CURRENT_DATE + 320, 'https://docs.local/vehicles/wp-caa-2202/insurance.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('33000000-0000-0000-0000-000000000004', '32000000-0000-0000-0000-000000000003', 'INSURANCE', 'INS-CP-3303-EXPIRED', CURRENT_DATE - 380, CURRENT_DATE - 15, 'https://docs.local/vehicles/cp-cab-3303/insurance-old.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('33000000-0000-0000-0000-000000000005', '32000000-0000-0000-0000-000000000004', 'FITNESS_CERTIFICATE', 'FIT-WP-4404-2026', CURRENT_DATE - 100, CURRENT_DATE + 265, 'https://docs.local/vehicles/wp-cac-4404/fitness.pdf', TRUE, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, meter_epoch, source_type, source_reference_id, recorded_at, received_at, created_by, correction_of_reading_id, correction_reason, idempotency_key, notes, created_at)
VALUES 
  ('34000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'ODOMETER', 42000.000, 'KILOMETER', 0, 'BASELINE', '32000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '30' DAY, CURRENT_TIMESTAMP - INTERVAL '30' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-001', 'Fleet onboarding baseline odometer reading', CURRENT_TIMESTAMP),
  ('34000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000001', 'ENGINE_HOURS', 2050.000, 'HOUR', 0, 'BASELINE', '32000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '30' DAY, CURRENT_TIMESTAMP - INTERVAL '30' DAY, '00000000-0000-0000-0000-000000000001', NULL, NULL, 'BASE-READ-002', 'Fleet onboarding baseline engine hours reading', CURRENT_TIMESTAMP)
ON CONFLICT (reading_id) DO NOTHING;

INSERT INTO maintenance_schedule (id, vehicle_id, maintenance_type, scheduled_start, scheduled_end, status, description, service_provider, cost, created_at, updated_at, created_by, updated_by)
VALUES 
  ('35000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'PREVENTIVE_SERVICE', CURRENT_TIMESTAMP - INTERVAL '15' DAY, CURRENT_TIMESTAMP - INTERVAL '14' DAY, 'COMPLETED', '40,000 km Scheduled Engine Service & Filter Change', 'Sathosa Motors Workshop', 45000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('35000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000003', 'CORRECTIVE_REPAIR', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'IN_PROGRESS', 'Pneumatic Brake Caliper Overhaul & Rotor Replacement', 'United Motors Workshop', 125000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

INSERT INTO lubricant_log (id, vehicle_id, fluid_type, quantity, unit, recorded_at, odometer_km, engine_hours, vendor_id, supplier_name, reference_number, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES 
  ('36000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001', 'ENGINE_OIL', 12.50, 'LITRE', CURRENT_TIMESTAMP - INTERVAL '15' DAY, 42000.00, 2050.00, NULL, 'Caltex Lanka Depot', 'LUB-REF-001', 'Engine oil top-up during scheduled PM service', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 4. DRIVERS, LICENSES, MEDICALS, DRUG TESTS, VIOLATIONS, EXCEPTIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO driver (id, employee_number, first_name, last_name, phone, email, status, active)
VALUES 
  ('40000000-0000-0000-0000-000000000001', 'DRV-001', 'Kasun', 'Fernando', '+94 77 555 1001', 'kasun.fernando@example.test', 'AVAILABLE', TRUE),
  ('40000000-0000-0000-0000-000000000002', 'DRV-002', 'Amara', 'Jayasinghe', '+94 77 555 1002', 'amara.jayasinghe@example.test', 'AVAILABLE', TRUE),
  ('40000000-0000-0000-0000-000000000003', 'DRV-003', 'Ruwan', 'Bandara', '+94 77 555 1003', 'ruwan.bandara@example.test', 'AVAILABLE', TRUE),
  ('40000000-0000-0000-0000-000000000004', 'DRV-004', 'Dinesh', 'Perera', '+94 77 555 1004', 'dinesh.perera@example.test', 'UNAVAILABLE', TRUE),
  ('40000000-0000-0000-0000-000000000005', 'DRV-005', 'Nimal', 'Siriwardena', '+94 77 555 1005', 'nimal.siriwardena@example.test', 'SUSPENDED', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_license (id, driver_id, license_number, license_class, issue_date, expiry_date, status, active, created_at, updated_at, created_by, updated_by)
VALUES 
  ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'B1234567', 'HEAVY', CURRENT_DATE - 730, CURRENT_DATE + 365, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'B7654321', 'LIGHT', CURRENT_DATE - 365, CURRENT_DATE + 730, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('41000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', 'B4567890', 'HEAVY', CURRENT_DATE - 500, CURRENT_DATE + 600, 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_medical_record (id, driver_id, assessment_date, valid_from, valid_until, fitness_status, vision_test_status, restrictions, examiner_or_provider, certificate_reference, remarks, active, created_at, updated_at, created_by, updated_by)
VALUES 
  ('42000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', CURRENT_DATE - 60, CURRENT_DATE - 60, CURRENT_DATE + 305, 'FIT', 'PASSED', 'None', 'National Transport Medical Institute (NTMI)', 'MED-CERT-2026-001', 'Annual commercial driver fitness certified', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data'),
  ('42000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', CURRENT_DATE - 90, CURRENT_DATE - 90, CURRENT_DATE + 275, 'FIT_WITH_RESTRICTIONS', 'PASSED_WITH_GLASSES', 'Corrective eye lenses required while driving', 'Asiri Health Diagnostics', 'MED-CERT-2026-002', 'Vision 6/6 with corrective lenses', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_drug_test (id, driver_id, test_type, scheduled_date, sample_collected_at, result_date, result, status, laboratory_or_provider, reference_number, remarks, return_to_duty_required, return_to_duty_cleared_at, active, created_at, updated_at, created_by, updated_by)
VALUES 
  ('43000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'RANDOM', CURRENT_DATE - 40, CURRENT_TIMESTAMP - INTERVAL '40' DAY, CURRENT_DATE - 38, 'NEGATIVE', 'COMPLETED', 'Lanka Hospitals Diagnostics', 'DT-REF-2026-001', 'Full panel negative screening', FALSE, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_exception (id, driver_id, exception_type, start_time, end_time, status, reason, remarks, created_at, updated_at, created_by, updated_by)
VALUES 
  ('45000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000004', 'MEDICAL_LEAVE', CURRENT_TIMESTAMP - INTERVAL '5' DAY, CURRENT_TIMESTAMP + INTERVAL '10' DAY, 'ACTIVE', 'Knee physiotherapy rehabilitation', 'Approved by Operations Head with medical certificate', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'sample-data', 'sample-data')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 5. ROUTES, STOPS, REVISIONS & DISRUPTIONS
-- -------------------------------------------------------------------------------------------------
INSERT INTO route (id, code, name, origin_location_id, destination_location_id, planned_distance_km, estimated_duration_minutes, active)
VALUES 
  ('50000000-0000-0000-0000-000000000001', 'RTE-CMB-KDY', 'Colombo Central to Kandy Depot', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 116, 240, TRUE),
  ('50000000-0000-0000-0000-000000000002', 'RTE-CMB-GLE', 'Colombo to Galle Southern Expressway', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 125, 150, TRUE),
  ('50000000-0000-0000-0000-000000000003', 'RTE-CMB-JAF', 'Colombo to Jaffna Northern Express Corridor', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 395, 480, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO route_stop (route_id, stop_order, location_id)
VALUES 
  ('50000000-0000-0000-0000-000000000001', 0, '20000000-0000-0000-0000-000000000004'),
  ('50000000-0000-0000-0000-000000000003', 0, '20000000-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;

INSERT INTO route_revision (id, route_id, revision_number, change_reason, planned_distance_km, estimated_duration_minutes, created_at, created_by)
VALUES 
  ('51000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 1, 'Initial baseline route definition', 116.00, 240, CURRENT_TIMESTAMP - INTERVAL '30' DAY, 'ops.planner'),
  ('51000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 1, 'Southern Expressway corridor optimized', 125.00, 150, CURRENT_TIMESTAMP - INTERVAL '30' DAY, 'ops.planner')
ON CONFLICT (id) DO NOTHING;

INSERT INTO route_disruption (id, route_id, disruption_type, severity, description, affected_segment, delay_estimate_minutes, alternative_instructions, status, reported_at, resolved_at, created_by)
VALUES 
  ('52000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'ROAD_CONSTRUCTION', 'MODERATE', 'Resurfacing near Kadawatha flyover approach', 'Kadawatha - Nittambuwa section', 25, 'Take Ambepussa bypass detour', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, NULL, 'ops.manager')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 6. TRIPS & OPERATIONAL EVENTS
-- -------------------------------------------------------------------------------------------------
INSERT INTO trip (id, trip_number, customer_id, department_id, project_id, route_id, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, required_vehicle_type_id, required_capacity_kg, cargo_description, passenger_count, customer_instructions, notes, vehicle_id, driver_id, actual_start_time, actual_end_time, start_odometer_km, end_odometer_km, completion_remarks, created_at, updated_at)
VALUES 
  ('60000000-0000-0000-0000-000000000001', 'TRIP-DEMO-001', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'NORMAL', 'DRAFT', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP + INTERVAL '5' DAY, CURRENT_TIMESTAMP + INTERVAL '5' DAY + INTERVAL '8' HOUR, '31000000-0000-0000-0000-000000000001', 2500, 'Packaged FMCG retail products', 1, 'Call receiver 1 hour before arrival', 'Draft booking created by client portal', NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('60000000-0000-0000-0000-000000000006', 'TRIP-DEMO-006', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'NORMAL', 'IN_PROGRESS', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP + INTERVAL '4' HOUR, '31000000-0000-0000-0000-000000000001', 3500, 'Consumer goods in active transit', 1, 'Direct delivery to Kandy Warehouse', 'Driver en-route. Checkpoint passed at Kurunegala.', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, NULL, 42500, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP),
  ('60000000-0000-0000-0000-000000000007', 'TRIP-DEMO-007', '10000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002', 'NORMAL', 'COMPLETED', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '6' HOUR, '31000000-0000-0000-0000-000000000002', 750, 'Standard retail store supplies', 1, 'Receipt signed by Galle supervisor', 'Completed on schedule without discrepancies', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR, 18000, 18125, 'Delivered in full and in good condition', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR)
ON CONFLICT (id) DO NOTHING;

INSERT INTO trip_status_history (id, trip_id, from_status, to_status, action, vehicle_id, driver_id, license_class, actor, details, occurred_at)
VALUES 
  ('61000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', 'DISPATCHED', 'IN_PROGRESS', 'TRIP_STARTED', '32000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'HEAVY', 'ops.manager', 'Trip journey started from Colombo Hub', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
  ('61000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000007', 'IN_PROGRESS', 'COMPLETED', 'TRIP_COMPLETED', '32000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'LIGHT', 'ops.manager', 'Delivered and signed off in Galle', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '5' HOUR)
ON CONFLICT (id) DO NOTHING;

INSERT INTO trip_operational_event (id, trip_id, event_type, occurred_at, location_id, location_description, checkpoint_type, delay_minutes, reason, incident_severity, remarks, recorded_by, created_at, updated_at)
VALUES 
  ('62000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', 'CHECKPOINT', CURRENT_TIMESTAMP - INTERVAL '110' MINUTE, '20000000-0000-0000-0000-000000000001', 'Colombo Depot Exit Gate', 'DEPARTURE_ORIGIN', NULL, NULL, NULL, 'Vehicle left hub on schedule', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('62000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000006', 'DELAY', CURRENT_TIMESTAMP - INTERVAL '60' MINUTE, NULL, 'Kadawatha Expressway Interchange', NULL, 30, 'HEAVY_TRAFFIC', NULL, 'Heavy highway congestion due to road construction', 'driver.app', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 7. FUEL MANAGEMENT & BUNKER STORAGE
-- -------------------------------------------------------------------------------------------------
INSERT INTO vendor (id, code, name, contact_person, phone, email, active)
VALUES 
  ('80000000-0000-0000-0000-000000000001', 'VEND-LIOC', 'Lanka IOC PLC', 'Sunil Wickramasinghe', '+94 11 244 8888', 'corporate@lioc.example', TRUE),
  ('80000000-0000-0000-0000-000000000002', 'VEND-CEYPETCO', 'Ceylon Petroleum Corporation', 'Anura Dissanayake', '+94 11 252 3456', 'sales@ceypetco.example', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fuel_price (id, vendor_id, fuel_type, effective_from, effective_to, unit_price, currency_code, active, created_at, updated_at)
VALUES 
  ('81000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'DIESEL', '2026-01-01', NULL, 310.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('81000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000001', 'PETROL_92', '2026-01-01', NULL, 365.0000, 'LKR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id)
VALUES 
  ('70000000-0000-0000-0000-000000000001', 'FUEL-CMB-INTERNAL', 'Colombo Hub Internal Bunker Station', 'INTERNAL', TRUE, NULL, '20000000-0000-0000-0000-000000000001'),
  ('70000000-0000-0000-0000-000000000002', 'FUEL-KDY-INTERNAL', 'Kandy Depot Internal Bunker Station', 'INTERNAL', TRUE, NULL, '20000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bunker_tank (id, fuel_station_id, tank_code, tank_name, fuel_type, capacity_liters, current_stock_liters, minimum_stock_liters, status, commissioned_at, active, created_at, updated_at)
VALUES 
  ('72000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'BNK-CMB-DSL-01', 'Colombo Hub Main Diesel Tank', 'DIESEL', 10000.000, 6415.000, 1000.000, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60' DAY, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('72000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', 'BNK-CMB-PET-01', 'Colombo Hub Petrol 92 Tank', 'PETROL_92', 5000.000, 2755.000, 500.000, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60' DAY, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO bunker_stock_movement (id, tank_id, movement_type, quantity_liters, resulting_balance_liters, reference_type, reference_id, occurred_at, created_by, reason, created_at)
VALUES 
  ('73000000-0000-0000-0000-000000000001', '72000000-0000-0000-0000-000000000001', 'OPENING_BALANCE', 2500.000, 2500.000, 'INITIAL_SETUP', '72000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '60' DAY, '00000000-0000-0000-0000-000000000001', 'Initial depot tank commissioning', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO bunker_dip_reading (id, tank_id, physical_quantity_liters, book_quantity_at_measurement, variance_quantity_liters, measured_at, measured_by, notes, created_at)
VALUES 
  ('74000000-0000-0000-0000-000000000001', '72000000-0000-0000-0000-000000000001', 6410.000, 6415.000, -5.000, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, '00000000-0000-0000-0000-000000000001', 'Daily dip reading. Minor normal temperature variance.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fuel_purchase (id, purchase_number, vendor_id, fuel_station_id, fuel_type, purchase_date, invoice_number, invoice_date, quantity, unit_price, subtotal, tax_rate, tax_amount, other_charges, total_amount, currency_code, status, reconciliation_status, received_quantity, quantity_variance, expected_unit_price, price_variance, destination_fuel_station_id, delivery_note_number, received_at, approved_by, approved_at, reconciled_by, reconciled_at, reconciliation_notes, reconciliation_reference, notes, created_by, created_at, updated_at)
VALUES 
  ('75000000-0000-0000-0000-000000000001', 'PO-FUEL-2026-001', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'DIESEL', CURRENT_DATE - 15, 'INV-LIOC-8891', CURRENT_DATE - 15, 4000.0000, 310.0000, 1240000.00, 0.0000, 0.00, 0.00, 1240000.00, 'LKR', 'RECONCILED', 'RECONCILED', 4000.0000, 0.0000, 310.0000, 0.00, '70000000-0000-0000-0000-000000000001', 'DN-LIOC-9912', CURRENT_TIMESTAMP - INTERVAL '15' DAY, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '16' DAY, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '14' DAY, 'Invoice fully verified against depot flowmeter', 'REC-REF-001', 'Direct bowser delivery to Colombo Main Diesel Tank', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '17' DAY, CURRENT_TIMESTAMP - INTERVAL '14' DAY)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, driver_id, fuel_type, quantity, unit_price, total_amount, station_id, odometer, engine_hours, issue_date_time, status, requested_by, authorized_by, authorization_date_time, notes, created_at, updated_at)
VALUES 
  ('76000000-0000-0000-0000-000000000001', 'FI-2026-0001', '32000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', 'DIESEL', 85.000, 310.0000, 26350.00, '70000000-0000-0000-0000-000000000001', 42500.000, 2100.000, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'ISSUED', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '3' HOUR, 'Pre-trip tank full top up for Kandy delivery trip TRIP-DEMO-006', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 8. FREIGHT ORDERS, CARGO MANIFESTS, LOAD PLANS & FREIGHT INSURANCE
-- -------------------------------------------------------------------------------------------------
INSERT INTO freight_order (id, order_number, customer_id, origin_location_id, destination_location_id, requested_pickup_at, requested_delivery_at, service_level, priority, special_handling_instructions, version, created_at, updated_at, created_by, updated_by)
VALUES 
  ('85000000-0000-0000-0000-000000000001', 'FO-2026-0001', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'EXPRESS', 'HIGH', 'Temperature sensitive medical vaccines and diagnostics', 0, CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '3' DAY, 'freight.planner', 'freight.planner'),
  ('85000000-0000-0000-0000-000000000002', 'FO-2026-0002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP + INTERVAL '3' DAY, 'STANDARD', 'NORMAL', 'General retail consumer hardware and appliances', 0, CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'freight.planner', 'freight.planner')
ON CONFLICT (id) DO NOTHING;

INSERT INTO freight_order_line (id, freight_order_id, description, quantity, line_order)
VALUES 
  ('86000000-0000-0000-0000-000000000001', '85000000-0000-0000-0000-000000000001', 'Insulin Vials Cold Packs (2C to 8C)', 50.0000, 0),
  ('86000000-0000-0000-0000-000000000002', '85000000-0000-0000-0000-000000000001', 'Laboratory Glass Test Tubes & Pipettes', 20.0000, 1),
  ('86000000-0000-0000-0000-000000000003', '85000000-0000-0000-0000-000000000002', 'Electric Water Heaters & Geysers', 30.0000, 0)
ON CONFLICT (id) DO NOTHING;


INSERT INTO cargo_manifest (id, manifest_number, freight_order_id, freight_order_number, version, created_at, updated_at, created_by, updated_by, finalized_at, finalized_by)
VALUES 
  ('87000000-0000-0000-0000-000000000001', 'MNF-2026-0001', '85000000-0000-0000-0000-000000000001', 'FO-2026-0001', 0, CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'freight.planner', 'freight.planner', CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'freight.planner')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cargo_manifest_item (id, cargo_manifest_id, freight_order_line_id, description, quantity, packing_information, commodity_classification, customs_applicable, customs_information, hazardous, hazardous_classification, hazardous_details, item_order, fragile, temperature_sensitive)
VALUES 
  ('88000000-0000-0000-0000-000000000001', '87000000-0000-0000-0000-000000000001', '86000000-0000-0000-0000-000000000001', 'Insulin Vials Cold Packs (2C to 8C)', 50.0000, 'Insulated Thermocol Boxes with Gel Packs', 'PHARMACEUTICAL', FALSE, NULL, FALSE, NULL, NULL, 0, FALSE, TRUE),
  ('88000000-0000-0000-0000-000000000002', '87000000-0000-0000-0000-000000000001', '86000000-0000-0000-0000-000000000002', 'Laboratory Glass Test Tubes & Pipettes', 20.0000, 'Corrugated Cardboard with Bubble Wrap', 'GLASSWARE', FALSE, NULL, FALSE, NULL, NULL, 1, TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO load_plan (id, load_plan_number, cargo_manifest_id, vehicle_id, notes, version, created_at, updated_at, created_by, updated_by, readiness_status, ready_at, ready_by)
VALUES 
  ('89000000-0000-0000-0000-000000000001', 'LP-2026-0001', '87000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000004', 'Validated Reefer loading layout for cold chain vaccines', 1, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'freight.planner', 'freight.planner', 'STRUCTURALLY_READY', CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'freight.planner')
ON CONFLICT (id) DO NOTHING;

INSERT INTO load_plan_item_placement (id, load_plan_id, manifest_item_id, placement_order, zone_reference, stack_group, container_reference, loading_sequence, special_handling_notes)
VALUES 
  ('89500000-0000-0000-0000-000000000001', '89000000-0000-0000-0000-000000000001', '88000000-0000-0000-0000-000000000001', 0, 'REEFER_ZONE_A', 'TEMP_STACK_01', 'CONT-COLD-01', 0, 'Keep cold at 4C'),
  ('89500000-0000-0000-0000-000000000002', '89000000-0000-0000-0000-000000000001', '88000000-0000-0000-0000-000000000002', 1, 'AMBIENT_ZONE_B', 'FRAGILE_STACK_01', 'CONT-BOX-02', 1, 'Handle with care - fragile glassware')
ON CONFLICT (id) DO NOTHING;

INSERT INTO freight_insurance_policy (id, policy_number, freight_order_id, cargo_manifest_id, insurance_provider, policy_type, coverage_amount, premium_amount, currency, valid_from, valid_until, status, version, created_at, updated_at, created_by, updated_by)
VALUES 
  ('89800000-0000-0000-0000-000000000001', 'POL-2026-0001', '85000000-0000-0000-0000-000000000001', '87000000-0000-0000-0000-000000000001', 'Sri Lanka Insurance Corporation (SLIC)', 'ALL_RISK_CARGO', 3500000.0000, 17500.0000, 'LKR', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP + INTERVAL '30' DAY, 'ACTIVE', 0, CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '3' DAY, 'freight.planner', 'freight.planner')
ON CONFLICT (id) DO NOTHING;

INSERT INTO freight_insurance_claim (id, claim_number, policy_id, freight_order_id, incident_reference, damage_description, claimed_amount, assessed_amount, assessment_notes, assessed_by, assessed_at, status, resolution_reason, version, created_at, updated_at, created_by, updated_by)
VALUES 
  ('89900000-0000-0000-0000-000000000001', 'CLM-2026-0001', '89800000-0000-0000-0000-000000000001', '85000000-0000-0000-0000-000000000001', 'INC-2026-001', 'Minor temperature excursion in Reefer Zone A due to sensor fluctuation', 45000.0000, 40000.0000, 'Assessed 2 affected boxes, approved claim compensation', 'claims.officer', CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'APPROVED', 'Approved by Senior Underwriter', 1, CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'freight.planner', 'claims.officer')
ON CONFLICT (id) DO NOTHING;

INSERT INTO freight_insurance_settlement (id, claim_id, settlement_reference, amount, currency, notes, settled_by, settled_at)
VALUES 
  ('89950000-0000-0000-0000-000000000001', '89900000-0000-0000-0000-000000000001', 'SETTLE-2026-001', 40000.0000, 'LKR', 'Bank wire transfer settlement processed to client account', 'finance.manager', CURRENT_TIMESTAMP - INTERVAL '12' HOUR)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- 9. NOTIFICATION RULES
-- -------------------------------------------------------------------------------------------------
INSERT INTO notification_rule (id, rule_code, name, event_type, channel, recipient_type, template_id, enabled, created_at, updated_at)
VALUES 
  ('95000000-0000-0000-0000-000000000001', 'RULE-TRIP-DISPATCH', 'Trip Dispatch Alert', 'TRIP_DISPATCHED', 'EMAIL', 'CUSTOMER', 'tpl-trip-dispatch-email', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('95000000-0000-0000-0000-000000000002', 'RULE-BUNKER-LOW-STOCK', 'Bunker Tank Minimum Stock Alert', 'BUNKER_LOW_STOCK', 'IN_APP', 'FLEET_MANAGER', 'tpl-bunker-low-inapp', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('95000000-0000-0000-0000-000000000003', 'RULE-DOC-EXPIRY', 'Compliance Document Expiry Warning', 'DOCUMENT_EXPIRING_SOON', 'EMAIL', 'COMPLIANCE_OFFICER', 'tpl-doc-expiry-email', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
