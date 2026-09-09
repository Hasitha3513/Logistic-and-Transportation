package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome;
import java.time.Instant;
import java.util.List;

/** Tracking-internal polling boundary. It is deliberately not exposed by a controller. */
public interface TrackingProviderIngestionPort {
    List<ProviderIngestionOutcome> ingest(
            ProviderConnectionId connectionId,
            String leaseOwner,
            List<NormalizedPositionCandidate> candidates,
            Instant receivedAt);
}
