package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.VehicleMileageQuery;
import com.transportlogistics.app.fleet.VehicleMileageSummary;
import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordManualVehicleReadingRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleMeterResetRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleReadingCorrectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class VehicleReadingSecurityIntegrationTest {

    private static final UUID VEHICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PASSWORD = "security-test-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private VehicleReadingUseCase readingUseCase;
    @MockBean private VehicleReadingRecorder readingRecorder;
    @MockBean private VehicleMileageQuery mileageQuery;

    private String viewToken;
    private String createToken;
    private String correctToken;
    private String resetToken;
    private String unprivilegedToken;

    @BeforeEach
    void setUpSecurity() throws Exception {
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        seedRoleAndUser("reading.viewer", "VEHICLE_READING_VIEW");
        seedRoleAndUser("reading.creator", "VEHICLE_READING_CREATE");
        seedRoleAndUser("reading.corrector", "VEHICLE_READING_CORRECT");
        seedRoleAndUser("reading.resetter", "VEHICLE_READING_RESET_METER");
        seedRoleAndUser("unprivileged");

        viewToken = login("reading.viewer");
        createToken = login("reading.creator");
        correctToken = login("reading.corrector");
        resetToken = login("reading.resetter");
        unprivilegedToken = login("unprivileged");
    }

    @Test
    void getReadingsSecurityRules() throws Exception {
        when(readingUseCase.list(any())).thenReturn(new VehicleReadingUseCase.PageResult<>(List.of(), 0, 20, 0L, 0));

        mvc.perform(get("/vehicles/{id}/readings", VEHICLE_ID)).andExpect(status().isUnauthorized());
        mvc.perform(get("/vehicles/{id}/readings", VEHICLE_ID).header("Authorization", "Bearer " + unprivilegedToken)).andExpect(status().isForbidden());
        mvc.perform(get("/vehicles/{id}/readings", VEHICLE_ID).header("Authorization", "Bearer " + viewToken)).andExpect(status().isOk());
    }

    @Test
    void postManualReadingSecurityRules() throws Exception {
        var sample = new VehicleReading(UUID.randomUUID(), VEHICLE_ID, VehicleReadingType.ODOMETER,
                new BigDecimal("10000.000"), VehicleReadingType.ODOMETER.unit(), 0, VehicleReadingSourceType.MANUAL,
                null, OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), null, null, "k1", null, OffsetDateTime.now());
        when(readingUseCase.record(any())).thenReturn(sample);

        var body = objectMapper.writeValueAsString(new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("10000.000"), OffsetDateTime.now(), "k1", "Test"
        ));

        mvc.perform(post("/vehicles/{id}/readings", VEHICLE_ID).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/vehicles/{id}/readings", VEHICLE_ID).header("Authorization", "Bearer " + viewToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/vehicles/{id}/readings", VEHICLE_ID).header("Authorization", "Bearer " + createToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    @Test
    void postCorrectionSecurityRules() throws Exception {
        var readingId = UUID.randomUUID();
        var sample = new VehicleReading(UUID.randomUUID(), VEHICLE_ID, VehicleReadingType.ODOMETER,
                new BigDecimal("10500.000"), VehicleReadingType.ODOMETER.unit(), 0, VehicleReadingSourceType.MANUAL,
                null, OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), readingId, "Reason", null, null, OffsetDateTime.now());
        when(readingUseCase.correct(any())).thenReturn(sample);

        var body = objectMapper.writeValueAsString(new RecordVehicleReadingCorrectionRequest(
                new BigDecimal("10500.000"), "Typo", OffsetDateTime.now()
        ));

        mvc.perform(post("/vehicles/{id}/readings/{readingId}/correct", VEHICLE_ID, readingId).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/vehicles/{id}/readings/{readingId}/correct", VEHICLE_ID, readingId).header("Authorization", "Bearer " + createToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/vehicles/{id}/readings/{readingId}/correct", VEHICLE_ID, readingId).header("Authorization", "Bearer " + correctToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    @Test
    void postMeterResetSecurityRules() throws Exception {
        var reset = new VehicleMeterReset(UUID.randomUUID(), VEHICLE_ID, VehicleReadingType.ODOMETER, 0, 1,
                new BigDecimal("250000.000"), new BigDecimal("0.000"), OffsetDateTime.now(), "Reset", UUID.randomUUID(), OffsetDateTime.now());
        when(readingUseCase.resetMeter(any())).thenReturn(reset);

        var body = objectMapper.writeValueAsString(new RecordVehicleMeterResetRequest(
                VehicleReadingType.ODOMETER, new BigDecimal("0.000"), OffsetDateTime.now(), "Reset odometer"
        ));

        mvc.perform(post("/vehicles/{id}/meter-resets", VEHICLE_ID).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/vehicles/{id}/meter-resets", VEHICLE_ID).header("Authorization", "Bearer " + createToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/vehicles/{id}/meter-resets", VEHICLE_ID).header("Authorization", "Bearer " + resetToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    @Test
    void getMileageSecurityRules() throws Exception {
        var summary = new VehicleMileageSummary(VEHICLE_ID, OffsetDateTime.now().minusDays(1), OffsetDateTime.now(),
                new BigDecimal("10000.000"), new BigDecimal("10100.000"), new BigDecimal("100.000"),
                null, null, null, 0, CoverageStatus.COMPLETE, false);
        when(readingUseCase.getMileage(eq(VEHICLE_ID), any(), any())).thenReturn(summary);

        mvc.perform(get("/vehicles/{id}/mileage", VEHICLE_ID)).andExpect(status().isUnauthorized());
        mvc.perform(get("/vehicles/{id}/mileage", VEHICLE_ID).header("Authorization", "Bearer " + unprivilegedToken)).andExpect(status().isForbidden());
        mvc.perform(get("/vehicles/{id}/mileage", VEHICLE_ID).header("Authorization", "Bearer " + viewToken)).andExpect(status().isOk());
    }

    private void seedRoleAndUser(String username, String... permissions) {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                roleId, "ROLE_" + username.toUpperCase().replace('.', '_'), "Role for " + username, true);

        for (String perm : permissions) {
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)", roleId, perm);
        }

        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, phone, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, username, username + "@example.test", passwords.encode(PASSWORD),
                "Test", "User", null, true, now, now);

        jdbc.update("INSERT INTO app_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private String login(String username) throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}