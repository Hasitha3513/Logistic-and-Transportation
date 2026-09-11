package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnectionMutation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingProviderConnectionStore {
    TrackingProviderConnection create(NewTrackingProviderConnection connection);

    Optional<TrackingProviderConnection> find(UUID tenantId, UUID connectionId);

    List<TrackingProviderConnection> list(UUID tenantId);

    TrackingProviderConnection update(
            UUID tenantId,
            UUID connectionId,
            long expectedVersion,
            TrackingProviderConnectionMutation mutation,
            UUID actorId,
            Instant now);
}
