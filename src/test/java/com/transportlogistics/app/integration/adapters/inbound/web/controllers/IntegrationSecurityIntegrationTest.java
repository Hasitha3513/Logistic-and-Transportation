package com.transportlogistics.app.integration.adapters.inbound.web.controllers;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import com.transportlogistics.app.integration.ports.inbound.IntegrationManagementUseCase;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationSecurityIntegrationTest.TenantTestConfiguration.class)
class IntegrationSecurityIntegrationTest {
    private static final UUID ID = UUID.fromString("a1000000-0000-0000-0000-000000000073");
    private static final UUID TENANT = UUID.fromString("b1000000-0000-0000-0000-000000000073");
    private static final String VERSION = "{\"version\":0}";
    private static final String CREATE = """
        {"name":"Controlled sandbox","type":"FILE_EXCHANGE","protocol":"FILE_JSON_V1",
         "direction":"OUTBOUND","endpointAlias":"CONTROLLED_SANDBOX",
         "dataClassification":"INTERNAL_OPERATIONAL_NON_SENSITIVE","mapping":{
           "mappingKey":"US73_PLATFORM_PROBE","sourceContract":"US73_PLATFORM_PROBE","sourceVersion":1,
           "targetSchema":"US73_FILE_PROBE","targetVersion":1,"rules":[
             {"sourceField":"probeId","targetField":"probe_id","format":"UUID","required":true},
             {"sourceField":"probeType","targetField":"probe_type","format":"ENUM","required":true},
             {"sourceField":"sequence","targetField":"sequence","format":"DECIMAL","required":true}]}}
        """;
    private static final String UPDATE = """
        {"name":"Controlled sandbox","endpointAlias":"CONTROLLED_SANDBOX","version":0,"mapping":{
           "mappingKey":"US73_PLATFORM_PROBE","sourceContract":"US73_PLATFORM_PROBE","sourceVersion":1,
           "targetSchema":"US73_FILE_PROBE","targetVersion":1,"rules":[
             {"sourceField":"probeId","targetField":"probe_id","format":"UUID","required":true},
             {"sourceField":"probeType","targetField":"probe_type","format":"ENUM","required":true},
             {"sourceField":"sequence","targetField":"sequence","format":"DECIMAL","required":true}]}}
        """;

    @Autowired MockMvc mvc;
    @Autowired CurrentTenant currentTenant;
    @Autowired IntegrationManagementUseCase useCase;

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantTestConfiguration {
        @Bean
        @Primary
        CurrentTenant integrationSecurityCurrentTenant() {
            return org.mockito.Mockito.mock(CurrentTenant.class);
        }

        @Bean
        @Primary
        IntegrationManagementUseCase integrationSecurityManagementUseCase() {
            return org.mockito.Mockito.mock(IntegrationManagementUseCase.class);
        }
    }

    @BeforeEach
    void setup() {
        var tenant = new TenantExecutionContext(TENANT, UUID.randomUUID(), "operator", "test-correlation");
        when(currentTenant.current()).thenReturn(Optional.of(tenant));
        when(currentTenant.required()).thenReturn(tenant);
        var view = view();
        when(useCase.list(any(), anyInt(), anyInt())).thenReturn(new IntegrationManagementUseCase.PageResult<>(
            List.of(view), 0, 20, 1, 1));
        when(useCase.create(any(), any())).thenReturn(view);
        when(useCase.get(any(), any())).thenReturn(view);
        when(useCase.update(any(), any(), any())).thenReturn(view);
        when(useCase.test(any(), any())).thenReturn(new IntegrationManagementUseCase.TestResult(view, true,
            "INTEGRATION_TEST_SUCCEEDED", OffsetDateTime.now()));
        when(useCase.enable(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(view);
        when(useCase.disable(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(view);
        when(useCase.exchanges(any(), any(), anyInt(), anyInt())).thenReturn(
            new IntegrationManagementUseCase.PageResult<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void literalAndEffectiveFormsOfAllEightRoutesDenyWrongAuthority() throws Exception {
        for (boolean literal : List.of(false, true)) {
            expect(HttpMethod.GET, "/v1/integrations", literal, null, 403);
            expect(HttpMethod.POST, "/v1/integrations", literal, CREATE, 403);
            expect(HttpMethod.GET, "/v1/integrations/" + ID, literal, null, 403);
            expect(HttpMethod.PUT, "/v1/integrations/" + ID, literal, UPDATE, 403);
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/test", literal, null, 403);
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/enable", literal, VERSION, 403);
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/disable", literal, VERSION, 403);
            expect(HttpMethod.GET, "/v1/integrations/" + ID + "/exchanges", literal, null, 403);
        }
    }

    @Test
    @WithMockUser(authorities = "INTEGRATION_VIEW")
    void viewPermissionAllowsOnlyReadConfigurationRoutesInBothForms() throws Exception {
        for (boolean literal : List.of(false, true)) {
            expect(HttpMethod.GET, "/v1/integrations", literal, null, 200);
            expect(HttpMethod.GET, "/v1/integrations/" + ID, literal, null, 200);
        }
    }

    @Test
    @WithMockUser(authorities = "INTEGRATION_MANAGE")
    void managePermissionAllowsCreateAndUpdateInBothForms() throws Exception {
        for (boolean literal : List.of(false, true)) {
            expect(HttpMethod.POST, "/v1/integrations", literal, CREATE, 201);
            expect(HttpMethod.PUT, "/v1/integrations/" + ID, literal, UPDATE, 200);
        }
    }

    @Test
    @WithMockUser(authorities = "INTEGRATION_TEST")
    void testPermissionAllowsTestInBothForms() throws Exception {
        for (boolean literal : List.of(false, true))
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/test", literal, null, 200);
    }

    @Test
    @WithMockUser(authorities = "INTEGRATION_ACTIVATE")
    void activatePermissionAllowsEnableAndDisableInBothForms() throws Exception {
        for (boolean literal : List.of(false, true)) {
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/enable", literal, VERSION, 200);
            expect(HttpMethod.POST, "/v1/integrations/" + ID + "/disable", literal, VERSION, 200);
        }
    }

    @Test
    @WithMockUser(authorities = "INTEGRATION_AUDIT_VIEW")
    void auditPermissionAllowsExchangeHistoryInBothForms() throws Exception {
        for (boolean literal : List.of(false, true))
            expect(HttpMethod.GET, "/v1/integrations/" + ID + "/exchanges", literal, null, 200);
    }

    private void expect(HttpMethod method, String path, boolean literal, String body, int expected) throws Exception {
        String uri = literal ? "/api" + path : path;
        MockHttpServletRequestBuilder builder = request(method, uri);
        if (literal) builder.contextPath("/api");
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(builder).andExpect(status().is(expected));
    }

    private IntegrationManagementUseCase.IntegrationView view() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID mappingId = UUID.randomUUID();
        var configuration = new IntegrationConfiguration(ID, TENANT, "Controlled sandbox", "CONTROLLED SANDBOX",
            IntegrationConfiguration.Type.FILE_EXCHANGE, IntegrationConfiguration.Protocol.FILE_JSON_V1,
            IntegrationConfiguration.Direction.OUTBOUND, "CONTROLLED_SANDBOX", null, mappingId,
            IntegrationConfiguration.DataClassification.INTERNAL_OPERATIONAL_NON_SENSITIVE,
            IntegrationConfiguration.RetryPolicy.US73_BOUNDED_V1, IntegrationConfiguration.Lifecycle.DRAFT,
            IntegrationConfiguration.Health.UNKNOWN, null, null, null, 0, now, "operator", now, "operator");
        var mapping = IntegrationMapping.active(TENANT, ID, "US73_PLATFORM_PROBE", 1,
            IntegrationMapping.PROBE_CONTRACT, 1, IntegrationMapping.PROBE_SCHEMA, 1, List.of(
                new IntegrationMapping.Rule("probeId", "probe_id", null, IntegrationMapping.Format.UUID, false, true),
                new IntegrationMapping.Rule("probeType", "probe_type", null, IntegrationMapping.Format.ENUM, false, true),
                new IntegrationMapping.Rule("sequence", "sequence", null, IntegrationMapping.Format.DECIMAL, false, true)),
            now, "operator");
        return new IntegrationManagementUseCase.IntegrationView(configuration, mapping);
    }
}
