package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import java.time.Instant;
import java.util.UUID;

public interface GpsExceptionManagementUseCase {
    GpsExceptionEpisode acknowledge(UUID tenantId, UUID episodeId, String reason, Instant acknowledgedAt);

    GpsExceptionEpisode resolveAfterCorrection(UUID tenantId, UUID episodeId, Instant correctedAt);
}
