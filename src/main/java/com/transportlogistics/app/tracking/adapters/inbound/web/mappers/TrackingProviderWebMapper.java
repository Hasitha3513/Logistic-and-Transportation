package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.TrackingProviderResponses;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderDescriptor;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrackingProviderWebMapper {
    default TrackingProviderResponses.ProviderType response(TrackingProviderDescriptor value) {
        return new TrackingProviderResponses.ProviderType(
                value.providerType().value(), names(value.capabilities().asSet()), value.supported());
    }

    default TrackingProviderResponses.Connection response(TrackingProviderConnection value) {
        return new TrackingProviderResponses.Connection(
                value.id().value(), value.providerType().value(), value.displayName(),
                value.providerAlias(), value.endpoint(), value.safeConfiguration().values(),
                value.credentialReference() != null, value.pollIntervalSeconds(), value.pageSize(),
                value.lifecycle().name(), value.testStatus().name(), value.lastTestedAt(),
                value.lastSuccessfulPollAt(), value.lastProviderMessageAt(),
                value.lastErrorCategory(), value.nextPollAt(), value.version());
    }

    default TrackingProviderResponses.ConnectionPage response(
            TrackingProviderManagementUseCase.Page<TrackingProviderConnection> value) {
        return new TrackingProviderResponses.ConnectionPage(
                value.items().stream().map(this::response).toList(),
                value.page(), value.size(), value.total());
    }

    default TrackingProviderResponses.DeviceBinding response(
            TrackingDeviceProviderBinding value) {
        return new TrackingProviderResponses.DeviceBinding(
                value.id(), value.trackingDeviceId(), value.providerConnectionId().value(),
                mask(value.externalDeviceReference()), value.safeConfiguration().values(),
                value.lifecycle().name(), value.watermarkSourceTimestamp(), value.nextPollAt(),
                value.version());
    }

    default TrackingProviderResponses.Discovery response(DiscoveryResult value) {
        return new TrackingProviderResponses.Discovery(
                value.status().name(), value.devices().stream().map(device ->
                        new TrackingProviderResponses.DiscoveredDevice(
                                mask(device.externalDeviceReference()), device.displayName(),
                                names(device.capabilities().asSet()))).toList(), value.nextCursor());
    }

    private static Set<String> names(
            Set<com.transportlogistics.app.tracking.application.provider.ProviderCapability> values) {
        return values.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    private static String mask(String value) {
        return value.length() < 5 ? "****" : "****" + value.substring(value.length() - 4);
    }
}
