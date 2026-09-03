package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.MaintenanceScheduleRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class FleetMaintenanceSecurityIntegrationTest {

    private static final String PASSWORD = "fleet-sec-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MaintenanceScheduleUseCase maintenanceScheduleUseCase;

    private String viewerToken;
    private String managerToken;
    private String unprivilegedToken;

    private final UUID vehicleId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurity() throws Exception {
        cleanupUsersAndRoles();

        seedRoleAndUser("fleet.viewer", "VEHICLE_VIEW");
        seedRoleAndUser("fleet.manager", "VEHICLE_VIEW", "VEHICLE_MAINTENANCE_MANAGE");
        seedRoleAndUser("unprivileged");

        viewerToken = login("fleet.viewer");
        managerToken = login("fleet.manager");
        unprivilegedToken = login("unprivileged");
    }

    private void cleanupUsersAndRoles() {
        jdbc.update("DELETE FROM refresh_token WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('fleet.viewer', 'fleet.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM tenant_membership_role WHERE membership_id IN (SELECT membership_id FROM tenant_membership WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('fleet.viewer', 'fleet.manager', 'unprivileged')))");
        jdbc.update("DELETE FROM tenant_membership WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('fleet.viewer', 'fleet.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM app_user_role WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('fleet.viewer', 'fleet.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM app_user WHERE username IN ('fleet.viewer', 'fleet.manager', 'unprivileged')");
        jdbc.update("DELETE FROM app_role_permission WHERE role_id IN (SELECT id FROM app_role WHERE name IN ('ROLE_FLEET_VIEWER', 'ROLE_FLEET_MANAGER', 'ROLE_UNPRIVILEGED'))");
        jdbc.update("DELETE FROM app_role WHERE name IN ('ROLE_FLEET_VIEWER', 'ROLE_FLEET_MANAGER', 'ROLE_UNPRIVILEGED')");
    }

    @Test
    void listMaintenanceSchedulesSecurityRules() throws Exception {
        when(maintenanceScheduleUseCase.list(vehicleId)).thenReturn(List.of());

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged
        mvc.perform(get("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for VEHICLE_VIEW
        mvc.perform(get("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .header("Authorization", "Bearer " + viewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createMaintenanceScheduleSecurityRules() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var request = new MaintenanceScheduleRequest("Service", start, end, "Desc", "Shop", new BigDecimal("100.00"));

        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service", start, end, MaintenanceStatus.SCHEDULED,
                "Desc", "Shop", new BigDecimal("100.00"), OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "fleet.manager", "fleet.manager"
        );

        when(maintenanceScheduleUseCase.create(eq(vehicleId), any(), any())).thenReturn(schedule);

        // 401 Unauthorized
        mvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for viewer without VEHICLE_MAINTENANCE_MANAGE
        mvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // 201 Created for manager with VEHICLE_MAINTENANCE_MANAGE
        mvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelMaintenanceScheduleSecurityRules() throws Exception {
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(2),
                MaintenanceStatus.CANCELLED, "Cancelled", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceScheduleUseCase.cancel(eq(vehicleId), eq(scheduleId), any(), any())).thenReturn(schedule);

        // 403 Forbidden for viewer
        mvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules/" + scheduleId + "/cancel")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // 200 OK for manager
        mvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules/" + scheduleId + "/cancel")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
        com.transportlogistics.app.support.TenantTestFixtures.canonicalMembership(jdbc, userId);

        com.transportlogistics.app.support.TenantTestFixtures.assignCanonicalRole(jdbc, userId, roleId);
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
