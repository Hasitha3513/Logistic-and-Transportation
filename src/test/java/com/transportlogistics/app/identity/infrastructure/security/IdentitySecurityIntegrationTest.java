package com.transportlogistics.app.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IdentitySecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;

    private UUID adminId;

    @BeforeEach
    void seedIdentity() {
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM fuel_purchase_history");
        jdbc.update("DELETE FROM fuel_purchase");
        jdbc.update("DELETE FROM bunker_stock_adjustment");
        jdbc.update("DELETE FROM bunker_dip_reading");
        jdbc.update("DELETE FROM bunker_stock_movement");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        adminId = UUID.randomUUID();
        var adminRoleId = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                adminRoleId, "ADMIN", "Administrator", true);
        jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)",
                adminRoleId, "IDENTITY_MANAGE");
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, phone, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                adminId, "admin", "admin@example.com", passwords.encode("correct-password"), "Admin", "User", null,
                true, now, now);
        jdbc.update("INSERT INTO app_user_role (user_id, role_id) VALUES (?, ?)", adminId, adminRoleId);
    }

    @Test
    void loginMeAuthorizationRefreshRotationAndLogoutWorkEndToEnd() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        var loginJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login.getResponse().getContentAsString());
        var access = loginJson.get("accessToken").asText();
        var refresh = loginJson.get("refreshToken").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/users").header("Authorization", "Bearer " + access)).andExpect(status().isOk());

        var rotated = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn();
        var rotatedJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rotated.getResponse().getContentAsString());
        var newRefresh = rotatedJson.get("refreshToken").asText();

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUsersCannotAuthenticateAndErrorsCarryCorrelationId() throws Exception {
        jdbc.update("UPDATE app_user SET active = FALSE WHERE id = ?", adminId);
        mvc.perform(post("/auth/login").header("X-Correlation-ID", "security-test-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "security-test-123"))
                .andExpect(jsonPath("$.correlationId").value("security-test-123"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void disabledUsersCannotContinueUsingPreviouslyIssuedAccessTokens() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk()).andReturn();
        var access = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        jdbc.update("UPDATE app_user SET active = FALSE WHERE id = ?", adminId);

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void permissionChangesAreCheckedAgainstCurrentRoleAssignments() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk()).andReturn();
        var access = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        jdbc.update("DELETE FROM app_role_permission");

        mvc.perform(get("/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void expiredRefreshTokensCannotBeUsed() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk()).andReturn();
        var refresh = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();
        jdbc.update("UPDATE refresh_token SET expires_at = ?", OffsetDateTime.parse("2025-01-01T00:00:00Z"));

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicAndProtectedRoutesUseExpectedSecurityPolicy() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }
}
