package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverExceptionRequest;
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
class FleetDriverExceptionSecurityIntegrationTest {

    private static final String PASSWORD = "driver-sec-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DriverExceptionUseCase driverExceptionUseCase;

    private String viewerToken;
    private String managerToken;
    private String unprivilegedToken;

    private final UUID driverId = UUID.randomUUID();
    private final UUID exceptionId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurity() throws Exception {
        cleanupUsersAndRoles();

        seedRoleAndUser("driver.viewer", "DRIVER_VIEW");
        seedRoleAndUser("driver.manager", "DRIVER_VIEW", "DRIVER_EXCEPTION_MANAGE");
        seedRoleAndUser("unprivileged");

        viewerToken = login("driver.viewer");
        managerToken = login("driver.manager");
        unprivilegedToken = login("unprivileged");
    }

    private void cleanupUsersAndRoles() {
        jdbc.update("DELETE FROM refresh_token WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('driver.viewer', 'driver.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM tenant_membership_role WHERE membership_id IN (SELECT membership_id FROM tenant_membership WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('driver.viewer', 'driver.manager', 'unprivileged')))");
        jdbc.update("DELETE FROM tenant_membership WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('driver.viewer', 'driver.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM app_user_role WHERE user_id IN (SELECT id FROM app_user WHERE username IN ('driver.viewer', 'driver.manager', 'unprivileged'))");
        jdbc.update("DELETE FROM app_user WHERE username IN ('driver.viewer', 'driver.manager', 'unprivileged')");
        jdbc.update("DELETE FROM app_role_permission WHERE role_id IN (SELECT id FROM app_role WHERE name IN ('ROLE_DRIVER_VIEWER', 'ROLE_DRIVER_MANAGER', 'ROLE_UNPRIVILEGED'))");
        jdbc.update("DELETE FROM app_role WHERE name IN ('ROLE_DRIVER_VIEWER', 'ROLE_DRIVER_MANAGER', 'ROLE_UNPRIVILEGED')");
    }

    @Test
    void listDriverExceptionsSecurityRules() throws Exception {
        when(driverExceptionUseCase.list(driverId)).thenReturn(List.of());

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/drivers/" + driverId + "/exceptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged
        mvc.perform(get("/drivers/" + driverId + "/exceptions")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for DRIVER_VIEW
        mvc.perform(get("/drivers/" + driverId + "/exceptions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createDriverExceptionSecurityRules() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var request = new DriverExceptionRequest("LEAVE", start, end, "Personal leave", "Handover");

        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.SCHEDULED,
                "Personal leave", "Handover", OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "driver.manager", "driver.manager"
        );

        when(driverExceptionUseCase.create(eq(driverId), any(), any())).thenReturn(exception);

        // 401 Unauthorized
        mvc.perform(post("/drivers/" + driverId + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for viewer without DRIVER_EXCEPTION_MANAGE
        mvc.perform(post("/drivers/" + driverId + "/exceptions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // 201 Created for manager with DRIVER_EXCEPTION_MANAGE
        mvc.perform(post("/drivers/" + driverId + "/exceptions")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelDriverExceptionSecurityRules() throws Exception {
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(2),
                DriverExceptionStatus.CANCELLED, "Cancelled", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptionUseCase.cancel(eq(driverId), eq(exceptionId), any(), any())).thenReturn(exception);

        // 403 Forbidden for viewer
        mvc.perform(post("/drivers/" + driverId + "/exceptions/" + exceptionId + "/cancel")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // 200 OK for manager
        mvc.perform(post("/drivers/" + driverId + "/exceptions/" + exceptionId + "/cancel")
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
