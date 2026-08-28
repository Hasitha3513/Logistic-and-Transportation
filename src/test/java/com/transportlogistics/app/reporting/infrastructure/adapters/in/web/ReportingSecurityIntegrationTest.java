package com.transportlogistics.app.reporting.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.reporting.application.ports.in.DriverAssignmentUseCase;
import com.transportlogistics.app.reporting.application.ports.in.TripReportUseCase;
import com.transportlogistics.app.reporting.application.ports.in.VehicleUtilizationUseCase;
import com.transportlogistics.app.reporting.application.ports.in.FreightReportUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ReportingSecurityIntegrationTest {

    private static final String PASSWORD = "reporting-sec-pass";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TripReportUseCase tripReportUseCase;
    @MockBean private DriverAssignmentUseCase driverAssignmentUseCase;
    @MockBean private VehicleUtilizationUseCase vehicleUtilizationUseCase;
    @MockBean private FreightReportUseCase freightReportUseCase;

    private String reportViewerToken;
    private String unprivilegedToken;
    private String freightViewerToken;
    private String freightExporterToken;

    @BeforeEach
    void setUpSecurity() throws Exception {
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM fuel_purchase_history");
        jdbc.update("DELETE FROM fuel_purchase");
        jdbc.update("DELETE FROM bunker_stock_adjustment");
        jdbc.update("DELETE FROM bunker_dip_reading");
        jdbc.update("DELETE FROM bunker_stock_movement");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        seedRoleAndUser("report.viewer", "REPORT_VIEW");
        seedRoleAndUser("unprivileged");
        seedRoleAndUser("freight.viewer", "FREIGHT_REPORT_VIEW");
        seedRoleAndUser("freight.exporter", "FREIGHT_REPORT_VIEW", "FREIGHT_REPORT_EXPORT");

        reportViewerToken = login("report.viewer");
        unprivilegedToken = login("unprivileged");
        freightViewerToken = login("freight.viewer");
        freightExporterToken = login("freight.exporter");
    }

    @Test
    void tripReportsSecurityRules() throws Exception {
        when(tripReportUseCase.getTripReport(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/reports/trips")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged user
        mvc.perform(get("/reports/trips")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for user with REPORT_VIEW
        mvc.perform(get("/reports/trips")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + reportViewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void driverAssignmentsSecurityRules() throws Exception {
        when(driverAssignmentUseCase.getDriverAssignmentReport(any(), any(), any()))
                .thenReturn(List.of());

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/reports/driver-assignments")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged user
        mvc.perform(get("/reports/driver-assignments")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for user with REPORT_VIEW
        mvc.perform(get("/reports/driver-assignments")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + reportViewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void vehicleUtilizationSecurityRules() throws Exception {
        when(vehicleUtilizationUseCase.getVehicleUtilizationReport(any(), any(), any()))
                .thenReturn(List.of());

        // 401 Unauthorized for unauthenticated
        mvc.perform(get("/reports/vehicle-utilization")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 403 Forbidden for unprivileged user
        mvc.perform(get("/reports/vehicle-utilization")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 200 OK for user with REPORT_VIEW
        mvc.perform(get("/reports/vehicle-utilization")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .header("Authorization", "Bearer " + reportViewerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void freightReportsUseDedicatedViewAndExportPermissions() throws Exception {
        when(freightReportUseCase.shipments(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(freightReportUseCase.exportCsv(any())).thenReturn("header\r\n".getBytes());

        var shipments = get("/reports/freight/shipments").param("fromDate", "2026-08-01")
                .param("toDate", "2026-08-10");
        mvc.perform(shipments).andExpect(status().isUnauthorized());
        mvc.perform(get("/reports/freight/shipments").param("fromDate", "2026-08-01").param("toDate", "2026-08-10")
                .header("Authorization", "Bearer " + unprivilegedToken)).andExpect(status().isForbidden());
        mvc.perform(get("/reports/freight/shipments").param("fromDate", "2026-08-01").param("toDate", "2026-08-10")
                .header("Authorization", "Bearer " + freightViewerToken)).andExpect(status().isOk());

        mvc.perform(get("/reports/freight/export").param("fromDate", "2026-08-01").param("toDate", "2026-08-10")
                .header("Authorization", "Bearer " + freightViewerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/reports/freight/export").param("fromDate", "2026-08-01").param("toDate", "2026-08-10")
                .header("Authorization", "Bearer " + freightExporterToken)).andExpect(status().isOk());
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
