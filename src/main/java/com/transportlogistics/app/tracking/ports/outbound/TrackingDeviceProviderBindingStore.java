package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingDeviceProviderBindingStore {
    TrackingDeviceProviderBinding create(NewTrackingDeviceProviderBinding binding);

    Optional<TrackingDeviceProviderBinding> find(UUID tenantId, UUID bindingId);

    Optional<TrackingDeviceProviderBinding> findActiveByDevice(UUID tenantId, UUID trackingDeviceId);

    Optional<TrackingDeviceProviderBinding> findCurrentByDevice(UUID tenantId, UUID trackingDeviceId);

    Optional<TrackingDeviceProviderBinding> findByExternalReference(
            UUID tenantId, ProviderConnectionId connectionId, String externalDeviceReference);

    List<TrackingDeviceProviderBinding> listByProviderConnection(
            UUID tenantId, ProviderConnectionId connectionId, int limit);

    TrackingDeviceProviderBinding updateLifecycle(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            DeviceProviderBindingLifecycle lifecycle,
            UUID actorId,
            Instant now);

    TrackingDeviceProviderBinding rebind(
            NewTrackingDeviceProviderBinding replacement, long expectedCurrentVersion);

    TrackingDeviceProviderBinding updateWatermark(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            Instant sourceTimestamp,
            String messageIdentity,
            Instant nextPollAt,
            UUID actorId,
            Instant now);

    TrackingDeviceProviderBinding updateNextPoll(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            Instant nextPollAt,
            UUID actorId,
            Instant now);
}
