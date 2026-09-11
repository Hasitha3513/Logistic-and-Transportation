package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeedMonitoringQuery {
    Optional<SpeedRule> rule(UUID tenantId, UUID ruleId);
    Page<SpeedRule> rules(UUID tenantId, SpeedRule.Scope scope, SpeedRule.Lifecycle lifecycle,
                          int page, int size);
    Optional<VehicleSpeedState> state(UUID tenantId, UUID vehicleId);
    Page<VehicleSpeedState> states(UUID tenantId, VehicleSpeedState.MonitoringState state,
                                   int page, int size);
    Optional<SpeedingEpisode> episode(UUID tenantId, UUID episodeId);
    CursorPage<SpeedingEpisode> episodes(UUID tenantId, UUID vehicleId, UUID driverId,
                                         Instant from, Instant to, String cursor, int limit);

    record Page<T>(List<T> items, int page, int size, long total) {
        public Page { items = List.copyOf(items); }
    }

    record CursorPage<T>(List<T> items, String nextCursor) {
        public CursorPage { items = List.copyOf(items); }
    }
}
