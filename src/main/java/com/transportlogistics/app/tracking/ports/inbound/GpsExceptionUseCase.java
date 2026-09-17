package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GpsExceptionUseCase {
    Page<GpsExceptionEpisode> episodes(Context context, Filter filter, String cursor, int limit);
    Optional<GpsExceptionEpisode> episode(Context context, UUID episodeId);
    Page<GpsExceptionEvidence> evidence(Context context, UUID episodeId, String cursor, int limit);
    Acknowledgement acknowledge(Context context, UUID episodeId, long expectedVersion,
            String reason, String idempotencyKey);

    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) { }
    record Filter(Instant from, Instant to, EpisodeStatus status, ExceptionType type,
                  Severity severity, UUID vehicleId, UUID deviceId) { }
    record Page<T>(List<T> items, String nextCursor) { }
    record Acknowledgement(UUID episodeId, EpisodeStatus status, Severity severity,
                           long version, Instant acknowledgedAt) { }
}
