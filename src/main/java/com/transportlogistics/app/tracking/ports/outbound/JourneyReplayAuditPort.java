package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.SelectionType;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface JourneyReplayAuditPort {
    void record(UUID tenantId, UUID actorId, String correlationId, String action,
                SelectionType selectorType, UUID selectorId, Duration requestedDuration,
                int resultCount, Coverage coverage, Set<OverlayType> overlays, Instant occurredAt);
}
