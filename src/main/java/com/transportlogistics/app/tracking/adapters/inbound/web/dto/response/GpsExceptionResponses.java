package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GpsExceptionResponses {
    private GpsExceptionResponses() { }
    public record Episode(UUID id,UUID deviceId,UUID vehicleId,String type,String severity,String status,
                          Instant openedAt,Instant lastObservedAt,Instant resolvedAt,long evidenceCount,
                          int consecutiveRecoveryPoints,long version) { }
    public record Evidence(UUID id,Instant sourceTimestamp,Instant assessedAt,String trust,String ordering,
                           String reliabilityState,List<String> qualityCodes,String transition) { }
    public record EpisodePage(List<Episode> items,String nextCursor) { }
    public record EvidencePage(List<Evidence> items,String nextCursor) { }
    public record Acknowledgement(UUID episodeId,String status,String severity,long version,Instant acknowledgedAt) { }
}
