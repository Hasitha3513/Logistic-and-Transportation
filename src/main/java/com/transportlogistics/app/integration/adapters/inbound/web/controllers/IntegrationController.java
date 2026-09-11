package com.transportlogistics.app.integration.adapters.inbound.web.controllers;

import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.CreateIntegrationRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.IntegrationVersionRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.UpdateIntegrationRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationExchangeResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationPageResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationTestResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.mappers.IntegrationWebMapper;
import com.transportlogistics.app.integration.ports.inbound.IntegrationManagementUseCase;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/integrations")
public class IntegrationController {
    private final IntegrationManagementUseCase integrations;
    private final IntegrationWebMapper mapper;
    private final CurrentTenant currentTenant;

    public IntegrationController(IntegrationManagementUseCase integrations, IntegrationWebMapper mapper,
                                 CurrentTenant currentTenant) {
        this.integrations = integrations;
        this.mapper = mapper;
        this.currentTenant = currentTenant;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    IntegrationPageResponse<IntegrationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       HttpServletRequest request) {
        return mapper.toIntegrationPage(integrations.list(context(request), page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    IntegrationResponse create(@Valid @RequestBody CreateIntegrationRequest body, HttpServletRequest request) {
        return mapper.toResponse(integrations.create(context(request), mapper.toCommand(body)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    IntegrationResponse get(@PathVariable UUID id, HttpServletRequest request) {
        return mapper.toResponse(integrations.get(context(request), id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    IntegrationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateIntegrationRequest body,
                               HttpServletRequest request) {
        return mapper.toResponse(integrations.update(context(request), id, mapper.toCommand(body)));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('INTEGRATION_TEST')")
    IntegrationTestResponse test(@PathVariable UUID id, HttpServletRequest request) {
        return mapper.toResponse(integrations.test(context(request), id));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('INTEGRATION_ACTIVATE')")
    IntegrationResponse enable(@PathVariable UUID id, @Valid @RequestBody IntegrationVersionRequest body,
                               HttpServletRequest request) {
        return mapper.toResponse(integrations.enable(context(request), id, body.version()));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('INTEGRATION_ACTIVATE')")
    IntegrationResponse disable(@PathVariable UUID id, @Valid @RequestBody IntegrationVersionRequest body,
                                HttpServletRequest request) {
        return mapper.toResponse(integrations.disable(context(request), id, body.version()));
    }

    @GetMapping("/{id}/exchanges")
    @PreAuthorize("hasAuthority('INTEGRATION_AUDIT_VIEW')")
    IntegrationPageResponse<IntegrationExchangeResponse> exchanges(@PathVariable UUID id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    HttpServletRequest request) {
        return mapper.toExchangePage(integrations.exchanges(context(request), id, page, size));
    }

    private IntegrationManagementUseCase.Context context(HttpServletRequest request) {
        var tenant = currentTenant.required();
        Object supplied = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String correlationId = supplied == null ? tenant.correlationId() : supplied.toString();
        return new IntegrationManagementUseCase.Context(tenant.tenantId(), tenant.username(), correlationId);
    }
}
