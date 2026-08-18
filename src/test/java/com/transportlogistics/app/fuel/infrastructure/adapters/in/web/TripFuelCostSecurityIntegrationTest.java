package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fuel.TripFuelCost;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import com.transportlogistics.app.fuel.application.ports.in.TripFuelCostUseCase;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class TripFuelCostSecurityIntegrationTest {

    private static final String PASSWORD = "security-test-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TripFuelCostUseCase tripFuelCostUseCase;

    private final UUID tripId = UUID.randomUUID();
    private String fuelCostViewToken;
    private String fuelIssueViewToken;
    private String unprivilegedToken;

    @BeforeEach
    void setUpSecurity() throws Exception {
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        seedRoleAndUser("cost.viewer", "FUEL_COST_VIEW");
        seedRoleAndUser("issue.viewer", "FUEL_ISSUE_VIEW");
        seedRoleAndUser("unprivileged");

        fuelCostViewToken = login("cost.viewer");
        fuelIssueViewToken = login("issue.viewer");
        unprivilegedToken = login("unprivileged");
    }

    @Test
    void getTripFuelCostSecurityRules() throws Exception {
        var cost = new TripFuelCost(
                tripId, UUID.randomUUID(), BigDecimal.ZERO, "LKR", BigDecimal.ZERO, null, null, null,
                0, 0, TripDistanceStatus.MISMATCH, TripFuelCostCalculationStatus.COMPLETE, List.of(), OffsetDateTime.now()
        );
        when(tripFuelCostUseCase.getTripFuelCost(eq(tripId))).thenReturn(cost);

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged user
        mvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for user with FUEL_COST_VIEW
        mvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .header("Authorization", "Bearer " + fuelCostViewToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 403 Forbidden for user with FUEL_ISSUE_VIEW only (must not implicitly authorize)
        mvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .header("Authorization", "Bearer " + fuelIssueViewToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
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