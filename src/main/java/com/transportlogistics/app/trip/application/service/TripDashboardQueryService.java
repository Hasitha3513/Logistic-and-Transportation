package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.TripDashboardQuery;
import com.transportlogistics.app.trip.application.ports.out.TripDashboardRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TripDashboardQueryService implements TripDashboardQuery {
    private final TripDashboardRepository repository;

    public TripDashboardQueryService(TripDashboardRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public List<ActiveTripContext> findActiveContexts(
            UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(vehicleIds, "vehicleIds");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
        if (vehicleIds.size() > MAXIMUM_VEHICLES) {
            throw new IllegalArgumentException("Dashboard Trip lookup is limited to 100 Vehicles");
        }
        if (vehicleIds.isEmpty()) {
            return List.of();
        }
        return List.copyOf(repository.findActiveContexts(tenantId, Set.copyOf(vehicleIds), evaluatedAt));
    }
}
