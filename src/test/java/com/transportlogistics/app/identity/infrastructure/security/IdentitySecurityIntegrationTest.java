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
    private static final UUID FOREIGN_TENANT_ID = UUID.fromString("b0000000-0000-0000-0000-000000000005");
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
        com.transportlogistics.app.support.TenantTestFixtures.canonicalMembership(jdbc, adminId);
        com.transportlogistics.app.support.TenantTestFixtures.assignCanonicalRole(jdbc, adminId, adminRoleId);
        jdbc.update("""
                INSERT INTO tenant (tenant_id, tenant_code, tenant_name, default_currency, default_time_zone, status,
                    created_at, created_by, updated_at, updated_by, version)
                VALUES (?, 'P005-FOREIGN', 'P0-05 Foreign Tenant', 'LKR', 'Asia/Colombo', 'ACTIVE', ?, 'test', ?, 'test', 0)
                ON CONFLICT (tenant_id) DO NOTHING
                """, FOREIGN_TENANT_ID, now, now);
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
        mvc.perform(get("/public/v1/delivery-self-service"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SELF_SERVICE_ACCESS_INVALID"));
        mvc.perform(get("/api/public/v1/delivery-self-service").contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SELF_SERVICE_ACCESS_INVALID"));
        mvc.perform(put("/api/public/v1/delivery-self-service/notification-preferences").contextPath("/api")
                        .header("Authorization", "DeliveryAccess AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":true,\"smsEnabled\":false,\"version\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SELF_SERVICE_ACCESS_INVALID"));
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdministratorCannotReadOrListForeignTenantUsers() throws Exception {
        var foreignUserId = insertForeignUser();
        var access = loginAsAdmin();

        mvc.perform(get("/users/{id}", foreignUserId).header("Authorization", "Bearer " + access))
                .andExpect(status().isNotFound());
        mvc.perform(get("/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'foreign-admin')]").isEmpty());
    }

    @Test
    void identityAdministratorCannotGrantPermissionsTheyDoNotHold() throws Exception {
        var access = loginAsAdmin();
        mvc.perform(post("/roles").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ESCALATED_ROLE\",\"description\":\"forbidden\",\"active\":true," +
                                "\"permissions\":[\"TRIP_APPROVE\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void identityAdministratorCannotModifyRoleAssignedToAnotherTenant() throws Exception {
        var foreignUserId = insertForeignUser();
        var foreignRole = UUID.randomUUID();
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, TRUE)",
                foreignRole, "FOREIGN_ADMIN_" + foreignRole, "Foreign tenant role");
        jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, 'IDENTITY_MANAGE')",
                foreignRole);
        jdbc.update("""
                INSERT INTO tenant_membership_role (membership_id, role_id)
                SELECT membership_id, ? FROM tenant_membership WHERE user_id = ?
                """, foreignRole, foreignUserId);

        mvc.perform(put("/roles/{id}", foreignRole).header("Authorization", "Bearer " + loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"FOREIGN_CHANGED\",\"description\":\"forbidden\",\"active\":true," +
                                "\"permissions\":[\"IDENTITY_MANAGE\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unmappedAuthenticatedRouteFailsClosed() throws Exception {
        mvc.perform(get("/unmapped-p005").header("Authorization", "Bearer " + loginAsAdmin()))
                .andExpect(status().isForbidden());
    }

    private String loginAsAdmin() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk()).andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private UUID insertForeignUser() {
        var userId = UUID.randomUUID();
        var membershipId = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, 'foreign-admin', ?, ?, 'Foreign', 'Admin', TRUE, ?, ?)",
                userId, "foreign-" + userId + "@example.com", passwords.encode("correct-password"), now, now);
        jdbc.update("""
                INSERT INTO tenant_membership (membership_id, tenant_id, user_id, status, created_at, created_by,
                    updated_at, updated_by, version) VALUES (?, ?, ?, 'ACTIVE', ?, 'test', ?, 'test', 0)
                """, membershipId, FOREIGN_TENANT_ID, userId, now, now);
        return userId;
    }
}
