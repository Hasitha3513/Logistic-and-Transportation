package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SpeedMonitoringResponses {
    private SpeedMonitoringResponses() { }
    public record Rule(UUID id, String name, String scope, UUID routeId, String routeVersion,
                       BigDecimal thresholdKph, String lifecycle, long version, Instant effectiveAt) { }
    public record RulePage(List<Rule> items, int page, int size, long total) { }
    public record State(UUID vehicleId, String monitoringState, String availability,
                        UUID effectiveRuleId, Long effectiveRuleVersion, UUID activeEpisodeId,
                        Instant lastEvaluatedSourceTimestamp) { }
    public record StatePage(List<State> items, int page, int size, long total) { }
    public record Episode(UUID episodeId, UUID vehicleId, UUID tripId, UUID driverId, UUID routeId,
                          String routeVersion, UUID ruleId, long ruleVersion, String thresholdSource,
                          BigDecimal effectiveThresholdKph, Instant startSourceTimestamp,
                          Instant confirmationSourceTimestamp, Instant endSourceTimestamp,
                          BigDecimal maxObservedSpeedKph, int eligibleAboveThresholdSampleCount,
                          String severity, int repeatCount) { }
    public record EpisodePage(List<Episode> items, String nextCursor) { }
}
