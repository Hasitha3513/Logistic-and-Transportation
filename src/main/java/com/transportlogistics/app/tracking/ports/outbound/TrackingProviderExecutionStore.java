package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingProviderExecutionStore {
    List<ClaimedProviderConnection> claimDueConnections(
            String leaseOwner, Instant now, Duration leaseDuration, int limit);

    boolean renewLease(
            ProviderConnectionId connectionId, String leaseOwner, Instant now, Duration leaseDuration);

    boolean hasActiveLease(ProviderConnectionId connectionId, String leaseOwner, Instant now);

    boolean releaseSuccess(
            ProviderConnectionId connectionId,
            String leaseOwner,
            Instant now,
            Instant nextPollAt,
            Instant lastProviderMessageAt);

    boolean releaseFailure(
            ProviderConnectionId connectionId,
            String leaseOwner,
            Instant now,
            Instant retryAt,
            String errorCategory);

    Optional<ClaimedProviderConnection> reloadActive(
            ProviderConnectionId connectionId, String leaseOwner, Instant now);

    List<TrackingDeviceProviderBinding> findDueActiveBindings(
            UUID tenantId, ProviderConnectionId connectionId, Instant now, int limit);

    Optional<TrackingDeviceProviderBinding> lockActiveBindingForIngestion(
            UUID tenantId,
            ProviderConnectionId connectionId,
            String externalDeviceReference,
            String leaseOwner,
            Instant now);
}
