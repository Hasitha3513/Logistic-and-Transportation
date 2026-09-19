package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IdleMonitoringResponses {
    private IdleMonitoringResponses(){ }
    public record State(UUID vehicleId,String vehicleLabel,String state,String capabilityState,
            Instant latestSourceTimestamp,Instant candidateStartedAt,Instant lastQualifyingAt,
            long creditedSeconds,int evidenceCount,long version,String fuelEstimateAvailability,
            String fuelEstimateSource){ }
    public record Episode(UUID id,UUID vehicleId,String vehicleLabel,String lifecycle,
            Instant startSourceTimestamp,Instant confirmedAt,Instant lastSourceTimestamp,
            Instant endSourceTimestamp,String endReason,long confirmedDurationSeconds,
            int evidenceCount,long version,String fuelEstimateAvailability,String fuelEstimateSource){ }
    public record Evidence(UUID id,Instant sourceTimestamp,String evidenceQuality,
            int creditedDeltaSeconds,Instant recordedAt){ }
    public record StatePage(List<State> items,String nextCursor){ }
    public record EpisodePage(List<Episode> items,String nextCursor){ }
    public record EvidencePage(List<Evidence> items,String nextCursor){ }
}
