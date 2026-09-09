package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.TrackingProviderRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.TrackingProviderResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.TrackingProviderWebMapper;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
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

@RestController
@RequestMapping("/v1/tracking")
public class TrackingProviderManagementController {
    private final TrackingProviderManagementUseCase useCase;
    private final TrackingProviderWebMapper mapper;
    private final CurrentTenant currentTenant;

    public TrackingProviderManagementController(
            TrackingProviderManagementUseCase useCase,
            TrackingProviderWebMapper mapper,
            CurrentTenant currentTenant) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.currentTenant = currentTenant;
    }

    @GetMapping("/provider-types")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public List<TrackingProviderResponses.ProviderType> providerTypes() {
        return useCase.providerTypes().stream().map(mapper::response).toList();
    }

    @PostMapping("/provider-connections")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection create(
            @Valid @RequestBody TrackingProviderRequests.CreateConnection request) {
        return mapper.response(useCase.create(context(),
                new TrackingProviderManagementUseCase.CreateConnection(
                        ProviderType.of(request.providerType()), request.displayName(),
                        request.providerAlias(), request.providerKeyId(), request.endpointUri(),
                        new ProviderSafeConfiguration(request.safeConfiguration()),
                        request.credentialReference(), request.pollIntervalSeconds(),
                        request.pageSize())));
    }

    @GetMapping("/provider-connections")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.ConnectionPage list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) ProviderConnectionLifecycle lifecycle,
            @RequestParam(required = false) ProviderConnectionTestStatus testStatus) {
        return mapper.response(useCase.list(tenant(), page, size,
                providerType == null ? null : ProviderType.of(providerType), lifecycle, testStatus));
    }

    @GetMapping("/provider-connections/{connectionId}")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection get(@PathVariable UUID connectionId) {
        return mapper.response(useCase.get(tenant(), new ProviderConnectionId(connectionId)));
    }

    @PutMapping("/provider-connections/{connectionId}")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection update(
            @PathVariable UUID connectionId,
            @Valid @RequestBody TrackingProviderRequests.UpdateConnection request) {
        return mapper.response(useCase.update(context(), new ProviderConnectionId(connectionId),
                new TrackingProviderManagementUseCase.UpdateConnection(
                        request.displayName(), request.endpointUri(),
                        new ProviderSafeConfiguration(request.safeConfiguration()),
                        request.credentialReference(), request.pollIntervalSeconds(), request.pageSize(),
                        request.version())));
    }

    @PostMapping("/provider-connections/{connectionId}/test")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.ConnectionTest test(@PathVariable UUID connectionId) {
        var result = useCase.test(context(), new ProviderConnectionId(connectionId));
        return new TrackingProviderResponses.ConnectionTest(
                mapper.response(result.connection()), result.result().status().name(),
                result.result().detailCode());
    }

    @PostMapping("/provider-connections/{connectionId}/activate")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection activate(
            @PathVariable UUID connectionId,
            @Valid @RequestBody TrackingProviderRequests.Version request) {
        return lifecycle(connectionId, request.version(), ProviderConnectionLifecycle.ACTIVE);
    }

    @PostMapping("/provider-connections/{connectionId}/disable")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection disable(
            @PathVariable UUID connectionId,
            @Valid @RequestBody TrackingProviderRequests.Version request) {
        return lifecycle(connectionId, request.version(), ProviderConnectionLifecycle.DISABLED);
    }

    @PostMapping("/provider-connections/{connectionId}/retire")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Connection retire(
            @PathVariable UUID connectionId,
            @Valid @RequestBody TrackingProviderRequests.Version request) {
        return lifecycle(connectionId, request.version(), ProviderConnectionLifecycle.RETIRED);
    }

    @GetMapping("/provider-connections/{connectionId}/devices/discover")
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.Discovery discover(
            @PathVariable UUID connectionId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam(required = false) String cursor) {
        return mapper.response(useCase.discover(
                context(), new ProviderConnectionId(connectionId), limit, cursor));
    }

    @PostMapping("/devices/{deviceId}/provider-bindings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')")
    public TrackingProviderResponses.DeviceBinding bind(
            @PathVariable UUID deviceId,
            @Valid @RequestBody TrackingProviderRequests.BindDevice request) {
        ProviderConnectionId connectionId = new ProviderConnectionId(request.providerConnectionId());
        ProviderSafeConfiguration configuration =
                new ProviderSafeConfiguration(request.safeConfiguration());
        if (request.currentBindingVersion() != null) {
            return mapper.response(useCase.rebind(context(),
                    new TrackingProviderManagementUseCase.RebindDevice(
                            deviceId, connectionId, request.externalDeviceReference(), configuration,
                            request.currentBindingVersion())));
        }
        DeviceProviderBindingLifecycle lifecycle = request.lifecycle() == null
                ? DeviceProviderBindingLifecycle.DRAFT
                : DeviceProviderBindingLifecycle.valueOf(request.lifecycle());
        return mapper.response(useCase.bind(context(),
                new TrackingProviderManagementUseCase.BindDevice(
                        deviceId, connectionId, request.externalDeviceReference(), configuration,
                        lifecycle)));
    }

    private TrackingProviderResponses.Connection lifecycle(
            UUID connectionId, long version, ProviderConnectionLifecycle lifecycle) {
        return mapper.response(useCase.lifecycle(
                context(), new ProviderConnectionId(connectionId), version, lifecycle));
    }

    private UUID tenant() {
        return currentTenant.required().tenantId();
    }

    private TrackingProviderManagementUseCase.Context context() {
        var current = currentTenant.required();
        return new TrackingProviderManagementUseCase.Context(
                current.tenantId(), current.actorId(), current.correlationId());
    }
}
