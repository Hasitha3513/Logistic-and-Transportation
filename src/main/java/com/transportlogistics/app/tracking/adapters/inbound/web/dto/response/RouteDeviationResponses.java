package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RouteDeviationResponses {
    private RouteDeviationResponses() { }
    public record Rule(UUID id, UUID routeId, String routeVersion, BigDecimal toleranceMeters,
                       String lifecycle, long ruleVersion, long version, Instant effectiveAt) { }
    public record RulePage(List<Rule> items, int page, int size, long total) { }
    public record State(UUID vehicleId, String state, String availability, UUID tripId,
                        UUID routeId, String routeVersion, UUID ruleId, long ruleVersion,
                        UUID activeEpisodeId, Instant lastSourceTimestamp) { }
    public record StatePage(List<State> items, int page, int size, long total) { }
    public record Episode(UUID id, UUID vehicleId, UUID tripId, UUID driverId, UUID routeId,
                          String routeVersion, UUID ruleId, long ruleVersion,
                          BigDecimal configuredToleranceMeters, BigDecimal effectiveToleranceMeters,
                          UUID firstCandidatePositionId, UUID confirmingPositionId,
                          Instant startSourceTimestamp, Instant confirmationSourceTimestamp,
                          Instant endSourceTimestamp, BigDecimal maximumDistanceMeters,
                          int eligibleOutsideSampleCount, String severity, String reviewStatus,
                          long reviewVersion, String terminalOutcome, UUID disruptionId) { }
    public record EpisodePage(List<Episode> items, String nextCursor) { }
    public record Review(UUID id, UUID episodeId, String status, String reason, String note,
                         UUID reviewerId, Instant reviewedAt, long reviewVersion,
                         UUID compensatesReviewId) { }
    public record ReviewPage(List<Review> items) { }
}
