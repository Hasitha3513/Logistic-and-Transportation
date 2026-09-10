package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderDescriptor;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface TrackingProviderManagementUseCase {
    List<TrackingProviderDescriptor> providerTypes();

    TrackingProviderConnection create(Context context, CreateConnection command);

    Page<TrackingProviderConnection> list(
            UUID tenantId, int page, int size, ProviderType providerType,
            ProviderConnectionLifecycle lifecycle, ProviderConnectionTestStatus testStatus);

    TrackingProviderConnection get(UUID tenantId, ProviderConnectionId connectionId);

    TrackingProviderConnection update(
            Context context, ProviderConnectionId connectionId, UpdateConnection command);

    TestConnectionResult test(Context context, ProviderConnectionId connectionId);

    TrackingProviderConnection lifecycle(
            Context context, ProviderConnectionId connectionId, long version,
            ProviderConnectionLifecycle lifecycle);

    DiscoveryResult discover(
            Context context, ProviderConnectionId connectionId, int limit, String cursor);

    TrackingDeviceProviderBinding bind(Context context, BindDevice command);

    TrackingDeviceProviderBinding rebind(Context context, RebindDevice command);

    java.util.Optional<TrackingDeviceProviderBinding> currentBinding(
            UUID tenantId, UUID trackingDeviceId);

    record Context(UUID tenantId, UUID actorId, String correlationId) { }

    record CreateConnection(
            ProviderType providerType,
            String displayName,
            String providerAlias,
            String providerKeyId,
            URI endpoint,
            ProviderSafeConfiguration safeConfiguration,
            String credentialReference,
            int pollIntervalSeconds,
            int pageSize) { }

    record UpdateConnection(
            String displayName,
            URI endpoint,
            ProviderSafeConfiguration safeConfiguration,
            String credentialReference,
            int pollIntervalSeconds,
            int pageSize,
            long version) { }

    record BindDevice(
            UUID trackingDeviceId,
            ProviderConnectionId providerConnectionId,
            String externalDeviceReference,
            ProviderSafeConfiguration safeConfiguration,
            DeviceProviderBindingLifecycle lifecycle) { }

    record RebindDevice(
            UUID trackingDeviceId,
            ProviderConnectionId providerConnectionId,
            String externalDeviceReference,
            ProviderSafeConfiguration safeConfiguration,
            long currentBindingVersion) { }

    record TestConnectionResult(
            TrackingProviderConnection connection, ConnectionTestResult result) { }

    record Page<T>(List<T> items, int page, int size, long total) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
