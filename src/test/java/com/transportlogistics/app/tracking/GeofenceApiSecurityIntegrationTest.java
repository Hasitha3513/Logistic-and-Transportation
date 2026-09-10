package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.adapters.inbound.security.SecuredGeofenceUseCases;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(GeofenceApiSecurityIntegrationTest.TenantStub.class)
class GeofenceApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("49000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.fromString("49000000-0000-0000-0000-000000000002");
    private static final String BODY = """
            {"name":"Restricted Yard","type":"UNAUTHORIZED_ZONE",
             "polygon":[{"longitude":79.8,"latitude":6.8},
                        {"longitude":79.9,"latitude":6.8},
                        {"longitude":79.9,"latitude":6.9}],
             "alertPolicy":{"alertOnEntry":true,"alertOnExit":false}}
            """;

    @Autowired MockMvc mvc;
    @Autowired SecuredGeofenceUseCases secured;

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantStub {
        @Bean
        @Primary
        CurrentTenant geofenceTestTenant() {
            TenantExecutionContext context =
                    new TenantExecutionContext(TENANT, ACTOR, "geofence.operator", "geofence-test");
            return new CurrentTenant() {
                @Override
                public Optional<TenantExecutionContext> current() {
                    return Optional.of(context);
                }
            };
        }
    }

    @Test
    @WithMockUser(authorities = "GEOFENCE_VIEW")
    void viewCanReadLiteralApiButCannotManageOrReadEvents() throws Exception {
        mvc.perform(get("/api/v1/tracking/geofences").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/geofences/memberships").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/geofences/transitions").contextPath("/api"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/geofences").contextPath("/api")
                        .header("Idempotency-Key", "security-view-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GEOFENCE_EVENT_VIEW")
    void eventViewCanReadBothHistoryRoutesButCannotManage() throws Exception {
        mvc.perform(get("/api/v1/tracking/geofences/transitions").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/geofences/unauthorized-transitions").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/tracking/geofences").contextPath("/api")
                        .header("Idempotency-Key", "security-event-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GEOFENCE_MANAGE")
    void manageCanCreateButDoesNotImplicitlyReceiveReadPermission() throws Exception {
        mvc.perform(post("/api/v1/tracking/geofences").contextPath("/api")
                        .header("Idempotency-Key", "security-manage-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/tracking/geofences").contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GEOFENCE_VIEW")
    void methodSecurityDeniesDirectManagementBoundaryCall() {
        var context = new GeofenceManagementUseCase.Context(TENANT, ACTOR, "direct", Instant.now());
        var command = new GeofenceManagementUseCase.CreateGeofence(
                "Direct", GeofenceType.UNAUTHORIZED_ZONE,
                GeofencePolygon.of(List.of(new Wgs84Coordinate(79.8, 6.8),
                        new Wgs84Coordinate(79.9, 6.8), new Wgs84Coordinate(79.9, 6.9))),
                null, new GeofenceAlertPolicy(true, false));
        assertThatThrownBy(() -> secured.create(context, command, "direct-method-key"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedLiteralApiIsDenied() throws Exception {
        mvc.perform(get("/api/v1/tracking/geofences").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }
}
