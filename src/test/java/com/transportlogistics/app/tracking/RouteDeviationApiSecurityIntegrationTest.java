package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.adapters.inbound.security.SecuredRouteDeviationUseCases;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationRuleManagementUseCase;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(RouteDeviationApiSecurityIntegrationTest.TenantStub.class)
class RouteDeviationApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("52000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.fromString("52000000-0000-0000-0000-000000000002");
    private static final UUID ROUTE = UUID.fromString("52000000-0000-0000-0000-000000000003");
    private static final String BODY = """
            {"routeId":"%s","routeVersion":"REVISION:1","toleranceMeters":150,
             "tenantId":"ffffffff-ffff-ffff-ffff-ffffffffffff",
             "actorId":"ffffffff-ffff-ffff-ffff-ffffffffffff"}
            """.formatted(ROUTE);
    @Autowired MockMvc mvc;
    @Autowired SecuredRouteDeviationUseCases secured;

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantStub {
        @Bean
        @Primary
        CurrentTenant routeDeviationTenant() {
            var context = new TenantExecutionContext(TENANT, ACTOR, "route.operator", "route-test");
            return new CurrentTenant() {
                @Override public Optional<TenantExecutionContext> current() {
                    return Optional.of(context);
                }
            };
        }
    }

    @Test
    @WithMockUser(authorities = "ROUTE_DEVIATION_VIEW")
    void viewReadsLiteralRulesAndStatesButCannotManageOrReadEpisodes() throws Exception {
        mvc.perform(get("/api/v1/tracking/route-deviations/rules").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/route-deviations/states").contextPath("/api"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/route-deviations/episodes").contextPath("/api")
                .param("from", "2026-09-01T00:00:00Z")
                .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/route-deviations/rules").contextPath("/api")
                .header("Idempotency-Key", "view-key").contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROUTE_DEVIATION_EVENT_VIEW")
    void eventViewReadsEpisodesButCannotReadRules() throws Exception {
        mvc.perform(get("/api/v1/tracking/route-deviations/episodes").contextPath("/api")
                .param("from", "2026-09-01T00:00:00Z")
                .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/route-deviations/rules").contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROUTE_DEVIATION_MANAGE")
    void manageCreatesWithTrustedTenantButDoesNotImplyView() throws Exception {
        mvc.perform(post("/api/v1/tracking/route-deviations/rules").contextPath("/api")
                .header("Idempotency-Key", "manage-security-key")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeId").value(ROUTE.toString()))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.actorId").doesNotExist());
        mvc.perform(get("/api/v1/tracking/route-deviations/rules").contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROUTE_DEVIATION_APPROVE")
    void approvePermissionDoesNotImplyManagementOrEventView() throws Exception {
        UUID episode = UUID.randomUUID();
        mvc.perform(post("/api/v1/tracking/route-deviations/episodes/" + episode + "/approve")
                .contextPath("/api").header("Idempotency-Key", "approve-security-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"reason\":\"ROAD_CLOSURE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/tracking/route-deviations/rules").contextPath("/api")
                .header("Idempotency-Key", "approve-manage-key")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tracking/route-deviations/episodes/" + episode)
                .contextPath("/api")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROUTE_DEVIATION_VIEW")
    void directUseCaseCannotBypassMethodSecurity() {
        var context = new RouteDeviationRuleManagementUseCase.Context(TENANT, ACTOR,
                "direct", Instant.now());
        assertThatThrownBy(() -> secured.create(context, ROUTE, "rev-direct",
                new BigDecimal("150"), "direct-security-key"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedLiteralApiIsDenied() throws Exception {
        mvc.perform(get("/api/v1/tracking/route-deviations/rules").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }
}
