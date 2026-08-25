package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.RoutePerformanceTripLookupPort;
import com.transportlogistics.app.routing.RouteTripMetric;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TripRoutePerformanceAdapter implements RoutePerformanceTripLookupPort {
    private final TripJpaRepository tripRepo;
    private final TripOperationalEventJpaRepository eventRepo;

    public TripRoutePerformanceAdapter(TripJpaRepository tripRepo, TripOperationalEventJpaRepository eventRepo) {
        this.tripRepo = tripRepo;
        this.eventRepo = eventRepo;
    }

    @Override
    public List<RouteTripMetric> findTripsForRoute(UUID routeId, OffsetDateTime from, OffsetDateTime to) {
        var entities = tripRepo.findByRouteIdAndDateRange(routeId, from, to);
        if (entities.isEmpty()) {
            return List.of();
        }
        var tripIds = entities.stream().map(TripEntity::getId).toList();
        var delayEvents = eventRepo.findByTripIdIn(tripIds);
        var delaysByTripId = delayEvents.stream()
                .filter(e -> e.getEventType() == TripOperationalEventType.DELAY && e.getDelayMinutes() != null)
                .collect(Collectors.groupingBy(
                        TripOperationalEventEntity::getTripId,
                        Collectors.summingInt(TripOperationalEventEntity::getDelayMinutes)
                ));

        return entities.stream().map(t -> new RouteTripMetric(
                t.getId(),
                t.getTripNumber(),
                t.getStatus(),
                t.getRequestedStartTime(),
                t.getRequestedEndTime(),
                t.getActualStartTime(),
                t.getActualEndTime(),
                t.getStartOdometerKm(),
                t.getEndOdometerKm(),
                delaysByTripId.getOrDefault(t.getId(), 0)
        )).toList();
    }
}
