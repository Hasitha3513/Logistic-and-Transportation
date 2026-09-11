package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeedingEpisodeRepositoryPort {
    SpeedingEpisode save(SpeedingEpisode episode);
    Optional<SpeedingEpisode> findEpisode(UUID tenantId, UUID episodeId);
    Optional<SpeedingEpisode> findActive(UUID tenantId, UUID vehicleId);
    Optional<SpeedingEpisode> findLatestClosed(UUID tenantId, UUID vehicleId,
                                                UUID ruleId, long ruleVersion);
    List<SpeedingEpisode> find(UUID tenantId, UUID vehicleId, UUID driverId,
                               Instant from, Instant to, String cursor, int limit);
}
