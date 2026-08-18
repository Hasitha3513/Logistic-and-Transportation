package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordManualVehicleReadingRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleMeterResetRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleReadingCorrectionRequest;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class VehicleReadingApiIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    private String token;
    private UUID vehicleId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");

        seedAdminIfNeeded();

        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var json = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        token = json.get("accessToken").asText();

        vehicleId = UUID.fromString("32000000-0000-0000-0000-000000000001");
        seedVehicleIfNeeded();
    }

    private void seedAdminIfNeeded() {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = 'admin'", Integer.class);
        if (count == null || count == 0) {
            var adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var adminRoleId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");

            var roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM app_role WHERE id = ?", Integer.class, adminRoleId);
            if (roleCount == null || roleCount == 0) {
                jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                        adminRoleId, "ADMIN", "Administrator", true);

                var permissions = new String[] {
                        "VEHICLE_READING_VIEW", "VEHICLE_READING_CREATE", "VEHICLE_READING_CORRECT", "VEHICLE_READING_RESET_METER"
                };
                for (String perm : permissions) {
                    jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)",
                            adminRoleId, perm);
                }
            }

            jdbc.update("""
                    INSERT INTO app_user
                        (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, adminId, "admin", "admin@transportlogistics.test", passwords.encode("AdminPass!2026"),
                    "Admin", "User", true, now, now);

            jdbc.update("INSERT INTO app_user_role (user_id, role_id) VALUES (?, ?)",
                    adminId, adminRoleId);
        }
    }

    private void seedVehicleIfNeeded() {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM vehicle WHERE id = ?", Integer.class, vehicleId);
        if (count == null || count == 0) {
            var categoryId = UUID.fromString("30000000-0000-0000-0000-000000000001");
            var typeId = UUID.fromString("31000000-0000-0000-0000-000000000001");

            var catCount = jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_category WHERE id = ?", Integer.class, categoryId);
            if (catCount == null || catCount == 0) {
                jdbc.update("INSERT INTO vehicle_category (id, code, name, description, active) VALUES (?, ?, ?, ?, ?)",
                        categoryId, "HEAVY", "Heavy Vehicle", "Heavy Vehicles", true);
            }
            var typeCount = jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_type WHERE id = ?", Integer.class, typeId);
            if (typeCount == null || typeCount == 0) {
                jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, description, active) VALUES (?, ?, ?, ?, ?, ?)",
                        typeId, categoryId, "TRUCK", "Prime Mover", "Prime Mover Truck", true);
            }

            jdbc.update("""
                    INSERT INTO vehicle
                        (id, registration_number, chassis_number, engine_number, category_id, type_id,
                         manufacturer, model, manufacture_year, ownership_type, operational_status,
                         current_odometer_km, engine_hours, capacity_kg, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, vehicleId, "WP-CAB-1234", "CHAS-1234", "ENG-1234", categoryId, typeId,
                    "Isuzu", "Giga", 2024, "OWNED", "AVAILABLE", 10000.0, 500.0, 25000.0, true);
        }
    }

    @Test
    void fullVehicleReadingCorrectionResetAndMileageLifecycle() throws Exception {
        var baseTime = OffsetDateTime.parse("2026-08-16T08:00:00Z");

        // 1. Record manual baseline reading 10,000 km
        var r1Body = objectMapper.writeValueAsString(new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("10000.000"), baseTime, "it-key-1", "Baseline"
        ));
        var r1Res = mvc.perform(post("/vehicles/{id}/readings", vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(r1Body))
                .andExpect(status().isCreated())
                .andReturn();
        var r1Json = objectMapper.readTree(r1Res.getResponse().getContentAsString());
        var r1Id = UUID.fromString(r1Json.get("id").asText());

        // 2. Record second reading 10,200 km
        var r2Body = objectMapper.writeValueAsString(new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("10200.000"), baseTime.plusHours(2), "it-key-2", "Stop A"
        ));
        mvc.perform(post("/vehicles/{id}/readings", vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(r2Body))
                .andExpect(status().isCreated());

        // 3. Attempt decreasing reading 10,100 km at later timestamp -> rejected with 409
        var rDecreasing = objectMapper.writeValueAsString(new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("10100.000"), baseTime.plusHours(3), "it-key-3", "Invalid decrease"
        ));
        mvc.perform(post("/vehicles/{id}/readings", vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rDecreasing))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_READING_DECREASE"));

        // 4. Correct first reading from 10,000 to 10,050
        var correctBody = objectMapper.writeValueAsString(new RecordVehicleReadingCorrectionRequest(
                new BigDecimal("10050.000"), "Typo corrected from log sheet", baseTime
        ));
        mvc.perform(post("/vehicles/{id}/readings/{readingId}/correct", vehicleId, r1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(10050.0))
                .andExpect(jsonPath("$.correctionOfReadingId").value(r1Id.toString()));

        // 5. Reset meter to 0 at baseTime + 4 hours
        var resetBody = objectMapper.writeValueAsString(new RecordVehicleMeterResetRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("0.000"), baseTime.plusHours(4), "Odometer replaced"
        ));
        mvc.perform(post("/vehicles/{id}/meter-resets", vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromEpoch").value(0))
                .andExpect(jsonPath("$.toEpoch").value(1));

        // 6. Record reading on new meter: 75 km
        var rNewMeter = objectMapper.writeValueAsString(new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("75.000"), baseTime.plusHours(6), "it-key-4", "New meter trip"
        ));
        mvc.perform(post("/vehicles/{id}/readings", vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rNewMeter))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meterEpoch").value(1))
                .andExpect(jsonPath("$.value").value(75.0));

        // 7. Verify mileage summary across resets: (10200 - 10050 = 150 km) + (75 - 0 = 75 km) = 225 km
        mvc.perform(get("/vehicles/{id}/mileage", vehicleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceTravelledKm").value(225.0))
                .andExpect(jsonPath("$.meterResetCount").value(1))
                .andExpect(jsonPath("$.coverageStatus").value("COMPLETE"));
    }
}