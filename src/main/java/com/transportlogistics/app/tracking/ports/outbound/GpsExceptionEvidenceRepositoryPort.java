package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GpsExceptionEvidenceRepositoryPort {
    boolean append(GpsExceptionEvidence evidence);
    List<GpsExceptionEvidence> findByEpisode(UUID tenantId, UUID episodeId,
            Instant afterTimestamp, UUID afterId, int limit);
}
