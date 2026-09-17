package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;

public interface GpsExceptionRepositoryPort {
    Optional<GpsExceptionEpisode> findActive(UUID tenantId, UUID deviceId, ExceptionType type);

    Optional<GpsExceptionEpisode> findActiveForUpdate(UUID tenantId, UUID deviceId, ExceptionType type);

    List<GpsExceptionEpisode> findActiveByDeviceForUpdate(UUID tenantId, UUID deviceId);

    Optional<GpsExceptionEpisode> findById(UUID tenantId, UUID episodeId);

    Optional<GpsExceptionEpisode> findByIdForUpdate(UUID tenantId, UUID episodeId);

    List<GpsExceptionEpisode> search(UUID tenantId, Instant from, Instant to,
            EpisodeStatus status, ExceptionType type, Severity severity, UUID vehicleId, UUID deviceId,
            Instant afterTimestamp, UUID afterId, int limit);

    List<GpsExceptionEpisode> findByTenant(UUID tenantId, int limit);

    GpsExceptionEpisode save(GpsExceptionEpisode episode);
}
