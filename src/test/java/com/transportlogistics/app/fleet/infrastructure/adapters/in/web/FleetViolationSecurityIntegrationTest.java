package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverPerformanceUseCase;
import com.transportlogistics.app.fleet.application.ports.in.DriverViolationUseCase;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverViolationRequest;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class FleetViolationSecurityIntegrationTest {

    private static final String PASSWORD = "violation-sec-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DriverViolationUseCase driverViolationUseCase;
    @MockBean private DriverPerformanceUseCase driverPerformanceUseCase;

    private String viewerToken;
    private String managerToken;
    private String unprivilegedToken;

    private final UUID driverId = UUID.randomUUID();
    private final UUID violationId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurity() throws Exception {
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        seedRoleAndUser("violation.viewer", "DRIVER_VIEW");
        seedRoleAndUser("violation.manager", "DRIVER_VIEW", "DRIVER_VIOLATION_MANAGE");
        seedRoleAndUser("unprivileged");

        viewerToken = login("violation.viewer");
        managerToken = login("violation.manager");
        unprivilegedToken = login("unprivileged");
    }

    @Test
    void listDriverViolationsSecurityRules() throws Exception {
        when(driverViolationUseCase.listViolations(driverId)).thenReturn(List.of());

        mvc.perform(get("/drivers/" + driverId + "/violations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/drivers/" + driverId + "/violations")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mvc.perform(get("/drivers/" + driverId + "/violations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void recordDriverViolationSecurityRules() throws Exception {
        var date = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var request = new DriverViolationRequest(
                null,
                DriverViolationType.SPEEDING,
                ViolationSeverity.MINOR,
                date,
                2,
                new BigDecimal("100.00"),
                "Highway",
                "Speeding"
        );

        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MINOR,
                date, 2, new BigDecimal("100.00"), FinePaymentStatus.UNPAID, null, null,
                "Highway", "Speeding", OffsetDateTime.now(), OffsetDateTime.now(), "violation.manager", "violation.manager"
        );

        when(driverViolationUseCase.recordViolation(any())).thenReturn(violation);

        mvc.perform(post("/drivers/" + driverId + "/violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/drivers/" + driverId + "/violations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/drivers/" + driverId + "/violations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getDriverPerformanceSecurityRules() throws Exception {
        var summary = new DriverPerformanceSummary(
                driverId, "Driver One", 10, 10, 0, 100.0, 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, 100, PerformanceRating.EXCELLENT, OffsetDateTime.now()
        );
        when(driverPerformanceUseCase.getPerformanceSummary(driverId)).thenReturn(summary);

        mvc.perform(get("/drivers/" + driverId + "/performance")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mvc.perform(get("/drivers/" + driverId + "/performance")
                        .header("Authorization", "Bearer " + viewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private void seedRoleAndUser(String username, String... permissions) {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                roleId, "ROLE_" + username.toUpperCase().replace('.', '_'), "Role for " + username, true);

        for (String perm : permissions) {
            jdbc.update("INSERT INTO app_permission (code, description, active) VALUES (?, 'Desc', true) ON CONFLICT DO NOTHING", perm);
            jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING", roleId, perm);
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
        var jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        if (jsonNode.has("accessToken")) {
            return jsonNode.get("accessToken").asText();
        }
        return jsonNode.get("token").asText();
    }
}
