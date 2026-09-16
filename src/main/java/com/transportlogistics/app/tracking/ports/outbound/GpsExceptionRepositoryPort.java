package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GpsExceptionRepositoryPort {
    Optional<GpsExceptionEpisode> findActive(UUID tenantId, UUID deviceId, ExceptionType type);

    Optional<GpsExceptionEpisode> findById(UUID tenantId, UUID episodeId);

    List<GpsExceptionEpisode> findByTenant(UUID tenantId, int limit);

    GpsExceptionEpisode save(GpsExceptionEpisode episode);
}
