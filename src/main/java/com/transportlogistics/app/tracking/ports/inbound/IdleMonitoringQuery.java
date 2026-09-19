package com.transportlogistics.app.tracking.ports.inbound;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdleMonitoringQuery {
    Page<State> states(Context context, StateFilter filter, String cursor, int limit);
    Page<Episode> episodes(Context context, EpisodeFilter filter, String cursor, int limit);
    Optional<Episode> episode(Context context, UUID episodeId);
    Page<Evidence> evidence(Context context, UUID episodeId, String cursor, int limit);

    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) { }
    record StateFilter(UUID vehicleId, String state) { }
    record EpisodeFilter(UUID vehicleId, Instant from, Instant to, String endReason) { }
    record Page<T>(List<T> items, String nextCursor) { }
    record State(UUID vehicleId, String state, String capabilityState, Instant latestSourceTimestamp,
                 Instant candidateStartedAt, Instant lastQualifyingAt, long creditedSeconds,
                 int evidenceCount, long version) { }
    record Episode(UUID id, UUID vehicleId, String lifecycle, Instant startSourceTimestamp,
                   Instant confirmedAt, Instant lastSourceTimestamp, Instant endSourceTimestamp,
                   String endReason, long creditedSeconds, int evidenceCount, long version) { }
    record Evidence(UUID id, Instant sourceTimestamp, String outcome, int creditedDeltaSeconds,
                    Instant recordedAt) { }
}
