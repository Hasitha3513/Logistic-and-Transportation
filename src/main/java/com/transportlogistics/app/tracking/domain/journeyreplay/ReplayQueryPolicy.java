package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ReplayQueryPolicy {
    public static final Duration DEFAULT_VEHICLE_RANGE = Duration.ofHours(6);
    public static final Duration MAX_RANGE = Duration.ofDays(7);

    private ReplayQueryPolicy() { }

    public static ReplayQuery validate(TenantContext tenant, ReplayRequest request, Instant now) {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(now, "now");
        ReplaySelector selector = selector(request.vehicleId(), request.tripId());
        if (request.exportRequested()) throw new JourneyReplayException(JourneyReplayError.UNSUPPORTED_EXPORT);
        if (request.overlays().contains(OverlayType.IDLE)) {
            throw new JourneyReplayException(JourneyReplayError.UNSUPPORTED_ENGINE_EVIDENCE);
        }
        TimeRange range = range(selector, request.from(), request.to(), now);
        int limit = request.limit() == null ? DEFAULT_POINT_LIMIT : request.limit();
        if (limit < 1 || limit > MAX_POINT_LIMIT) {
            throw new JourneyReplayException(JourneyReplayError.INVALID_PAGE_SIZE);
        }
        String cursor = request.cursor();
        if (cursor != null && cursor.isBlank()) throw new JourneyReplayException(JourneyReplayError.INVALID_CURSOR);
        return new ReplayQuery(tenant, selector, range, range, limit, cursor,
                Set.copyOf(request.overlays()), Direction.CHRONOLOGICAL_ASCENDING, BROWSER_POINT_CEILING);
    }

    public static void validateCursor(CursorState cursor, ReplayQuery query, Instant now) {
        if (cursor == null || cursor.binding() == null || cursor.position() == null
                || cursor.expiresAt() == null || !cursor.expiresAt().isAfter(now)) {
            throw new JourneyReplayException(JourneyReplayError.INVALID_CURSOR);
        }
        CursorBinding binding = cursor.binding();
        CursorPosition position = cursor.position();
        if (binding.tenantId() == null || binding.selector() == null
                || binding.requestedRange() == null || binding.snapshotRecordedAt() == null
                || position.sourceTimestamp() == null || position.historyId() == null) {
            throw new JourneyReplayException(JourneyReplayError.INVALID_CURSOR);
        }
        if (!binding.tenantId().equals(query.tenant().tenantId())
                || !binding.selector().equals(query.selector())
                || !binding.requestedRange().equals(query.requestedRange())) {
            throw new JourneyReplayException(JourneyReplayError.CURSOR_QUERY_MISMATCH);
        }
    }

    public static ProducerAcceptance acceptance(OverlayType overlay) {
        return switch (overlay) {
            case GEOFENCE -> ProducerAcceptance.ACCEPTED;
            case SPEED -> ProducerAcceptance.FIELD_FIDELITY_PENDING;
            case ROUTE_DEVIATION -> ProducerAcceptance.FIELD_ACCEPTANCE_PENDING;
            case IDLE -> throw new JourneyReplayException(JourneyReplayError.UNSUPPORTED_ENGINE_EVIDENCE);
        };
    }

    public static List<JourneyPoint> orderedDistinct(List<JourneyPoint> points) {
        LinkedHashMap<java.util.UUID, JourneyPoint> identities = new LinkedHashMap<>();
        points.stream().sorted().forEach(point -> identities.putIfAbsent(point.historyId(), point));
        return List.copyOf(identities.values());
    }

    private static ReplaySelector selector(java.util.UUID vehicleId, java.util.UUID tripId) {
        if ((vehicleId == null) == (tripId == null)) {
            throw new JourneyReplayException(JourneyReplayError.INVALID_SELECTOR);
        }
        return vehicleId != null ? ReplaySelector.vehicle(vehicleId) : ReplaySelector.trip(tripId);
    }

    private static TimeRange range(ReplaySelector selector, Instant from, Instant to, Instant now) {
        if (from == null && to == null && selector.type() == SelectionType.VEHICLE) {
            return new TimeRange(now.minus(DEFAULT_VEHICLE_RANGE), now);
        }
        if (from == null || to == null || !from.isBefore(to) || to.isAfter(now)) {
            throw new JourneyReplayException(JourneyReplayError.INVALID_RANGE);
        }
        TimeRange range = new TimeRange(from, to);
        if (range.duration().compareTo(MAX_RANGE) > 0) {
            throw new JourneyReplayException(JourneyReplayError.RANGE_TOO_LARGE);
        }
        return range;
    }
}
