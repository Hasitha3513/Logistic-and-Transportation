package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class TripFuelCostApiIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    private String token;
    private UUID tripId;
    private UUID vehicleId;
    private UUID vendorId;
    private UUID stationId;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        seedAdminIfNeeded();

        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var root = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        token = root.has("accessToken") ? root.get("accessToken").asText() : root.get("token").asText();

        setupTestData();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM trip_status_history");
        jdbc.update("DELETE FROM trip_dispatch");
        jdbc.update("DELETE FROM trip");
    }

    private void seedAdminIfNeeded() {
        var count = jdbc.queryForObject("SELECT count(*) FROM app_user WHERE username = 'admin'", Integer.class);
        if (count == null || count == 0) {
            var userId = UUID.randomUUID();
            var roleId = UUID.randomUUID();
            var now = OffsetDateTime.now();
            jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, 'ADMIN', 'Admin', true)", roleId);
            jdbc.update("INSERT INTO app_permission (code, description, active) VALUES ('FUEL_COST_VIEW', 'View cost', true) ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO app_permission (code, description, active) VALUES ('FUEL_ISSUE_VIEW', 'View issue', true) ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, 'FUEL_COST_VIEW') ON CONFLICT DO NOTHING", roleId);
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, 'FUEL_ISSUE_VIEW') ON CONFLICT DO NOTHING", roleId);
            jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, 'admin', 'admin@example.com', ?, 'Admin', 'User', true, ?, ?)",
                    userId, passwords.encode("AdminPass!2026"), now, now);
            com.transportlogistics.app.support.TenantTestFixtures.canonicalMembership(jdbc, userId);
            com.transportlogistics.app.support.TenantTestFixtures.assignCanonicalRole(jdbc, userId, roleId);
        } else {
            var roleId = jdbc.queryForObject("SELECT id FROM app_role WHERE name = 'ADMIN'", UUID.class);
            jdbc.update("INSERT INTO app_permission (code, description, active) VALUES ('FUEL_COST_VIEW', 'View cost', true) ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO app_permission (code, description, active) VALUES ('FUEL_ISSUE_VIEW', 'View issue', true) ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, 'FUEL_COST_VIEW') ON CONFLICT DO NOTHING", roleId);
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, 'FUEL_ISSUE_VIEW') ON CONFLICT DO NOTHING", roleId);
        }
    }

    private void setupTestData() {
        var catId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        var locOriginId = UUID.randomUUID();
        var locDestId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        jdbc.update("INSERT INTO vehicle_category (id, code, name, active) VALUES (?, 'CAT-C1', 'Cat 1', true) ON CONFLICT DO NOTHING", catId);
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, active) VALUES (?, ?, 'TYP-T1', 'Type 1', true) ON CONFLICT DO NOTHING", typeId, catId);
        jdbc.update("INSERT INTO vehicle (id, registration_number, category_id, type_id, ownership_type, operational_status, active) VALUES (?, 'REG-COST-1', ?, ?, 'OWNED', 'AVAILABLE', true)",
                vehicleId, catId, typeId);

        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, 'LOC-O', 'Origin', true) ON CONFLICT DO NOTHING", locOriginId);
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, 'LOC-D', 'Dest', true) ON CONFLICT DO NOTHING", locDestId);

        jdbc.update("""
                INSERT INTO trip (id, trip_number, priority, status, origin_location_id, destination_location_id, vehicle_id, requested_start_time, requested_end_time, created_at, updated_at)
                VALUES (?, 'TRIP-COST-01', 'MEDIUM', 'COMPLETED', ?, ?, ?, ?, ?, ?, ?)
                """, tripId, locOriginId, locDestId, vehicleId, now.minusHours(4), now, now, now);

        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, 'VEND-01', 'Shell', true)", vendorId);
        jdbc.update("INSERT INTO fuel_station (id, code, name, station_type, active, vendor_id, location_id) VALUES (?, 'ST-01', 'Shell Main', 'EXTERNAL', true, ?, ?)",
                stationId, vendorId, locOriginId);

        var adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", UUID.class);

        // Start odometer reading: 10000 km
        jdbc.update("""
                INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, recorded_at, received_at, source_type, source_reference_id, meter_epoch, created_by, created_at)
                VALUES (?, ?, 'ODOMETER', 10000.000, 'KILOMETER', ?, ?, 'TRIP_START', ?, 0, ?, ?)
                """, UUID.randomUUID(), vehicleId, now.minusHours(4), now.minusHours(4), tripId, adminId, now.minusHours(4));

        // Fuel issue 1 during trip: 20 L @ 300 = 6000
        jdbc.update("""
                INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, fuel_type, quantity, unit_price, total_amount, station_id, status, requested_by, created_at, updated_at, issue_date_time)
                VALUES (?, 'V-001', ?, ?, 'DIESEL', 20.000, 300.00, 6000.00, ?, 'ISSUED', ?, ?, ?, ?)
                """, UUID.randomUUID(), vehicleId, tripId, stationId, adminId, now.minusHours(3), now.minusHours(3), now.minusHours(3));

        // Fuel issue 2 during trip: 10 L @ 310 = 3100
        jdbc.update("""
                INSERT INTO fuel_issue (id, voucher_number, vehicle_id, trip_id, fuel_type, quantity, unit_price, total_amount, station_id, status, requested_by, created_at, updated_at, issue_date_time)
                VALUES (?, 'V-002', ?, ?, 'DIESEL', 10.000, 310.00, 3100.00, ?, 'ISSUED', ?, ?, ?, ?)
                """, UUID.randomUUID(), vehicleId, tripId, stationId, adminId, now.minusHours(2), now.minusHours(2), now.minusHours(2));

        // End odometer reading: 10200 km (distance = 200 km)
        jdbc.update("""
                INSERT INTO vehicle_reading (reading_id, vehicle_id, reading_type, value, unit, recorded_at, received_at, source_type, source_reference_id, meter_epoch, created_by, created_at)
                VALUES (?, ?, 'ODOMETER', 10200.000, 'KILOMETER', ?, ?, 'TRIP_END', ?, 0, ?, ?)
                """, UUID.randomUUID(), vehicleId, now, now, tripId, adminId, now);
    }

    @Test
    void getTripFuelCostCalculatesAuthoritativeCostAndPerKm() throws Exception {
        mvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.totalFuelQuantityLiters").value(30.000))
                .andExpect(jsonPath("$.totalFuelCost").value(9100.00))
                .andExpect(jsonPath("$.tripDistanceKm").value(200.000))
                .andExpect(jsonPath("$.costPerKm").value(45.50))
                .andExpect(jsonPath("$.litersPer100Km").value(15.00))
                .andExpect(jsonPath("$.fuelIssueCount").value(2))
                .andExpect(jsonPath("$.unpricedIssueCount").value(0))
                .andExpect(jsonPath("$.distanceStatus").value("CALCULATED"))
                .andExpect(jsonPath("$.calculationStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }
}
