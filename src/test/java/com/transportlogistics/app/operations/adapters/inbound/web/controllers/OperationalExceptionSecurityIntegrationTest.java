package com.transportlogistics.app.operations.adapters.inbound.web.controllers;

import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.operations.sla.enabled=false")
@AutoConfigureMockMvc
@Import(OperationalExceptionSecurityIntegrationTest.TestBeans.class)
class OperationalExceptionSecurityIntegrationTest {
    private static final UUID CASE_ID = UUID.fromString("78000000-0000-0000-0000-000000000078");
    private static final String VERSION = "{\"expectedVersion\":0}";
    private static final String REASON = "{\"expectedVersion\":0,\"reason\":\"Required reason\"}";
    @Autowired MockMvc mvc;
    @Autowired CurrentTenant currentTenant;
    @Autowired OperationalExceptionUseCase operations;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean @Primary CurrentTenant operationalSecurityTenant() { return org.mockito.Mockito.mock(CurrentTenant.class); }
        @Bean @Primary OperationalExceptionUseCase operationalSecurityUseCase() {
            return org.mockito.Mockito.mock(OperationalExceptionUseCase.class);
        }
    }

    @BeforeEach
    void setup() {
        var tenant = new TenantExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "operator", "security-test");
        when(currentTenant.current()).thenReturn(Optional.of(tenant));
        when(currentTenant.required()).thenReturn(tenant);
        when(operations.list(any(), any())).thenReturn(new OperationalExceptionUseCase.PageResult<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void everyLiteralAndEffectiveFrozenRouteDeniesUnrelatedAuthority() throws Exception {
        for (boolean literal : List.of(false, true)) {
            expect(HttpMethod.GET, "/v1/operational-exceptions", literal, null, 403);
            expect(HttpMethod.GET, path(""), literal, null, 403);
            expect(HttpMethod.GET, path("/history"), literal, null, 403);
            expect(HttpMethod.POST, path("/classify"), literal,
                "{\"expectedVersion\":0,\"category\":\"SAFETY\",\"severity\":\"HIGH\",\"reason\":\"reason\"}", 403);
            expect(HttpMethod.POST, path("/acknowledge"), literal, VERSION, 403);
            expect(HttpMethod.POST, path("/assign"), literal,
                "{\"expectedVersion\":0,\"assignmentType\":\"ROLE_QUEUE\",\"roleCode\":\"OPERATIONS_QUEUE\",\"reason\":\"reason\"}", 403);
            expect(HttpMethod.POST, path("/start"), literal, VERSION, 403);
            expect(HttpMethod.POST, path("/escalate"), literal, REASON, 403);
            expect(HttpMethod.POST, path("/corrective-actions"), literal,
                "{\"expectedVersion\":0,\"type\":\"CORRECTIVE\",\"description\":\"action\",\"ownerType\":\"ROLE_QUEUE\",\"ownerRoleCode\":\"OPERATIONS_QUEUE\"}", 403);
            expect(HttpMethod.POST, path("/corrective-actions/" + CASE_ID + "/start"), literal, VERSION, 403);
            expect(HttpMethod.POST, path("/corrective-actions/" + CASE_ID + "/complete"), literal, VERSION, 403);
            expect(HttpMethod.POST, path("/rca"), literal,
                "{\"expectedVersion\":0,\"causeCategory\":\"PROCESS\",\"rootCauseCode\":\"GAP\",\"summary\":\"summary\"}", 403);
            expect(HttpMethod.POST, path("/rca/approve"), literal,
                "{\"expectedCaseVersion\":0,\"expectedRcaVersion\":0}", 403);
            expect(HttpMethod.POST, path("/resolve"), literal,
                "{\"expectedVersion\":0,\"resolutionNote\":\"resolved\"}", 403);
            expect(HttpMethod.POST, path("/close"), literal, VERSION, 403);
            expect(HttpMethod.POST, path("/reject-resolution"), literal, REASON, 403);
            expect(HttpMethod.POST, path("/reopen"), literal, REASON, 403);
        }
    }

    @Test
    @WithMockUser(authorities = "OPERATIONAL_EXCEPTION_VIEW")
    void literalApiListRequiresAndAcceptsViewPermission() throws Exception {
        expect(HttpMethod.GET, "/v1/operational-exceptions", true, null, 200);
    }

    private static String path(String suffix) { return "/v1/operational-exceptions/" + CASE_ID + suffix; }
    private void expect(HttpMethod method, String path, boolean literal, String body, int expected) throws Exception {
        String uri = literal ? "/api" + path : path;
        MockHttpServletRequestBuilder builder = request(method, uri);
        if (literal) builder.contextPath("/api");
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(builder).andExpect(status().is(expected));
    }
}
