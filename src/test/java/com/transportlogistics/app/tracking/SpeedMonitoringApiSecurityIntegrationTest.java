package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.adapters.inbound.security.SecuredSpeedMonitoringUseCases;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
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
@Import(SpeedMonitoringApiSecurityIntegrationTest.TenantStub.class)
class SpeedMonitoringApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT=UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR=UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final String BODY="""
            {"name":"Tenant operational threshold","scope":"TENANT","thresholdKph":80}
            """;
    @Autowired MockMvc mvc;
    @Autowired SecuredSpeedMonitoringUseCases secured;
    @TestConfiguration(proxyBeanMethods=false) static class TenantStub {
        @Bean @Primary CurrentTenant speedTenant() {
            var context=new TenantExecutionContext(TENANT,ACTOR,"speed.operator","speed-test");
            return new CurrentTenant(){@Override public Optional<TenantExecutionContext> current(){return Optional.of(context);}};
        }
    }
    @Test @WithMockUser(authorities="SPEED_MONITOR_VIEW")
    void viewReadsLiteralRulesAndStatesButCannotManageOrReadEvents() throws Exception {
        mvc.perform(get("/api/v1/tracking/speed-monitoring/rules").contextPath("/api")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/speed-monitoring/states").contextPath("/api")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/speed-monitoring/episodes").contextPath("/api").param("from","2026-09-01T00:00:00Z").param("to","2026-09-02T00:00:00Z")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/speed-monitoring/rules").contextPath("/api").header("Idempotency-Key","view-key").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(authorities="SPEED_EVENT_VIEW")
    void eventViewReadsEpisodesButCannotReadRules() throws Exception {
        mvc.perform(get("/api/v1/tracking/speed-monitoring/episodes").contextPath("/api").param("from","2026-09-01T00:00:00Z").param("to","2026-09-02T00:00:00Z")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/speed-monitoring/rules").contextPath("/api")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(authorities="SPEED_MONITOR_MANAGE")
    void manageCreatesButDoesNotImplyView() throws Exception {
        mvc.perform(post("/api/v1/tracking/speed-monitoring/rules").contextPath("/api").header("Idempotency-Key","manage-key").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/tracking/speed-monitoring/rules").contextPath("/api")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(authorities="SPEED_MONITOR_VIEW")
    void directUseCaseCannotBypassMethodSecurity() {
        var context=new SpeedRuleManagementUseCase.Context(TENANT,ACTOR,"direct",Instant.now());
        var command=new SpeedRuleManagementUseCase.CreateRule("Direct",SpeedRule.Scope.TENANT,null,null,SpeedKph.threshold(java.math.BigDecimal.valueOf(80)));
        assertThatThrownBy(()->secured.create(context,command,"direct-key")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void unauthenticatedLiteralApiIsDenied() throws Exception {
        mvc.perform(get("/api/v1/tracking/speed-monitoring/rules").contextPath("/api")).andExpect(status().isUnauthorized());
    }
}
