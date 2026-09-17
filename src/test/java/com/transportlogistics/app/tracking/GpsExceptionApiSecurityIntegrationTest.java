package com.transportlogistics.app.tracking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(GpsExceptionApiSecurityIntegrationTest.TenantStub.class)
class GpsExceptionApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("55000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.fromString("55000000-0000-0000-0000-000000000002");
    private static final UUID FOREIGN_TENANT = UUID.fromString("55000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantStub {
        @Bean
        @Primary
        CurrentTenant gpsExceptionTenant() {
            var context = new TenantExecutionContext(TENANT, ACTOR, "gps.reviewer", "gps-api-test");
            return new CurrentTenant() {
                @Override
                public Optional<TenantExecutionContext> current() {
                    return Optional.of(context);
                }
            };
        }
    }

    @Test
    void unauthenticatedLiteralApiIsDenied() throws Exception {
        mvc.perform(get("/api/v1/tracking/gps-exceptions").contextPath("/api")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "GPS_EXCEPTION_VIEW")
    void viewCanListButCannotAcknowledge() throws Exception {
        mvc.perform(get("/api/v1/tracking/gps-exceptions").contextPath("/api")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items").isArray());
        mvc.perform(post("/api/v1/tracking/gps-exceptions/{id}/acknowledge", UUID.randomUUID())
                        .contextPath("/api")
                        .header("Idempotency-Key", "view-cannot-review-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"Reviewed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GPS_EXCEPTION_REVIEW")
    void reviewDoesNotImplyViewAndAcknowledgesWithStoredReplay() throws Exception {
        UUID episode = episode(TENANT, "review");
        String key = "acknowledge-security-001";
        String request = "{\"expectedVersion\":0,\"reason\":\"Dispatcher reviewed\"}";
        mvc.perform(get("/api/v1/tracking/gps-exceptions/{id}", episode).contextPath("/api"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/gps-exceptions/{id}/acknowledge", episode)
                        .contextPath("/api").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.reason").doesNotExist());
        mvc.perform(post("/api/v1/tracking/gps-exceptions/{id}/acknowledge", episode)
                        .contextPath("/api").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @WithMockUser(authorities = "GPS_EXCEPTION_VIEW")
    void foreignTenantEpisodeIsSafeNotFound() throws Exception {
        UUID foreignEpisode = episode(FOREIGN_TENANT, "foreign");
        mvc.perform(get("/api/v1/tracking/gps-exceptions/{id}", foreignEpisode)
                        .contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("GPS exception not found"));
    }

    private UUID episode(UUID tenantId, String suffix) {
        UUID deviceId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'GENERIC','ACTIVE',?,?,0,?,?)
                """, deviceId, tenantId, "gps-api-" + suffix, Timestamp.from(now), ACTOR,
                Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                INSERT INTO tracking_gps_exception_episode(
                 id,tenant_id,tracking_device_id,exception_type,severity,status,opened_at,
                 last_observed_at,evidence_count,consecutive_recovery_points,version)
                VALUES(?,?,?,'LOW_ACCURACY','WARNING','OPEN',?,?,1,0,0)
                """, episodeId, tenantId, deviceId, Timestamp.from(now), Timestamp.from(now));
        return episodeId;
    }
}
