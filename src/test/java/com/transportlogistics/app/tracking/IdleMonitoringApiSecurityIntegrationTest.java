package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.adapters.inbound.security.SecuredIdleMonitoringQuery;
import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc @Import(IdleMonitoringApiSecurityIntegrationTest.TenantStub.class)
class IdleMonitoringApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT=UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR=UUID.fromString("51000000-0000-0000-0000-000000000002");
    private static final UUID FOREIGN=UUID.fromString("51000000-0000-0000-0000-000000000003");
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @Autowired SecuredIdleMonitoringQuery securedQuery;
    @TestConfiguration(proxyBeanMethods=false) static class TenantStub {@Bean @Primary CurrentTenant idleTenant(){var c=new TenantExecutionContext(TENANT,ACTOR,"dispatcher","idle-api-test");return new CurrentTenant(){@Override public Optional<TenantExecutionContext> current(){return Optional.of(c);}};}}

    @Test void literalApiPathRequiresAuthentication()throws Exception{mvc.perform(get("/api/v1/tracking/idle-monitoring/states").contextPath("/api")).andExpect(status().isUnauthorized());}
    @Test @WithMockUser void directUseCaseInvocationRequiresPermission(){
        var context=new IdleMonitoringQuery.Context(TENANT,ACTOR,"direct-use-case",Instant.now());
        assertThatThrownBy(() -> securedQuery.states(context,new IdleMonitoringQuery.StateFilter(null,null),null,50))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test @WithMockUser(authorities="IDLE_MONITOR_VIEW") void monitorPermissionCanViewStatesButNotEpisodes()throws Exception{
        mvc.perform(get("/api/v1/tracking/idle-monitoring/states").contextPath("/api")).andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL,"no-store"));
        mvc.perform(get("/api/v1/tracking/idle-monitoring/episodes").contextPath("/api").param("from","2026-09-01T00:00:00Z").param("to","2026-09-02T00:00:00Z")).andExpect(status().isForbidden());}
    @Test @WithMockUser(authorities="IDLE_EVENT_VIEW") void eventPermissionIsTenantScopedAndMinimized()throws Exception{
        UUID local=episode(TENANT,"local"),foreign=episode(FOREIGN,"foreign");
        mvc.perform(get("/api/v1/tracking/idle-monitoring/episodes/{id}",local).contextPath("/api")).andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL,"no-store")).andExpect(jsonPath("$.vehicleId").exists()).andExpect(jsonPath("$.deviceId").doesNotExist()).andExpect(jsonPath("$.coordinates").doesNotExist());
        mvc.perform(get("/api/v1/tracking/idle-monitoring/episodes/{id}",foreign).contextPath("/api")).andExpect(status().isNotFound());}
    @Test @WithMockUser(authorities="IDLE_EVENT_VIEW") void episodeHistoryExcludesCandidatesAndEnforcesRangeBoundary()throws Exception{
        UUID confirmed=episode(TENANT,"confirmed-list");
        candidate(TENANT,"candidate-list");
        mvc.perform(get("/api/v1/tracking/idle-monitoring/episodes").contextPath("/api")
                        .param("from","2026-08-02T12:00:00Z").param("to","2026-09-02T12:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(confirmed.toString()));
        mvc.perform(get("/api/v1/tracking/idle-monitoring/episodes").contextPath("/api")
                        .param("from","2026-08-02T11:59:59Z").param("to","2026-09-02T12:00:00Z"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND action='IDLE_EPISODES_VIEWED'",Integer.class,TENANT)).isOne();
    }
    private UUID episode(UUID tenant,String suffix){Instant at=Instant.parse("2026-09-01T12:00:00Z");UUID device=UUID.randomUUID(),id=UUID.randomUUID(),vehicle=UUID.randomUUID();jdbc.update("INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,registered_by,version,created_at,updated_at) VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)",device,tenant,"idle-api-"+suffix,Timestamp.from(at),ACTOR,Timestamp.from(at),Timestamp.from(at));jdbc.update("INSERT INTO tracking_idle_episode(id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,credited_seconds,evidence_count) VALUES(?,?,?,?,'CONFIRMED',?,?,?,300,2)",id,tenant,vehicle,device,Timestamp.from(at),Timestamp.from(at.plusSeconds(300)),Timestamp.from(at.plusSeconds(300)));return id;}
    private void candidate(UUID tenant,String suffix){Instant at=Instant.parse("2026-09-01T12:00:00Z");UUID device=UUID.randomUUID();jdbc.update("INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,registered_by,version,created_at,updated_at) VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)",device,tenant,"idle-api-"+suffix,Timestamp.from(at),ACTOR,Timestamp.from(at),Timestamp.from(at));jdbc.update("INSERT INTO tracking_idle_episode(id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,last_source_timestamp,candidate_id) VALUES(?,?,?,?,'CANDIDATE',?,?,?)",UUID.randomUUID(),tenant,UUID.randomUUID(),device,Timestamp.from(at),Timestamp.from(at),UUID.randomUUID());}
}
