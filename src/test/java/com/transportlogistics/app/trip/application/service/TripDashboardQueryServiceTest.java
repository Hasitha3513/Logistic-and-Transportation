package com.transportlogistics.app.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.trip.TripDashboardQuery.ActiveTripContext;
import com.transportlogistics.app.trip.application.ports.out.TripDashboardRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripDashboardQueryServiceTest {
    @Test
    void performsOneBoundedTenantQualifiedBulkLookup() {
        UUID tenantId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant at = Instant.parse("2026-09-16T04:00:00Z");
        var repository = mock(TripDashboardRepository.class);
        var expected = new ActiveTripContext(vehicleId, UUID.randomUUID(), "DISPATCHED", null, null);
        when(repository.findActiveContexts(tenantId, Set.of(vehicleId), at)).thenReturn(List.of(expected));

        var result = new TripDashboardQueryService(repository)
                .findActiveContexts(tenantId, Set.of(vehicleId), at);

        assertThat(result).containsExactly(expected);
        verify(repository).findActiveContexts(tenantId, Set.of(vehicleId), at);
    }

    @Test
    void rejectsMoreThanOneHundredVehiclesBeforePersistence() {
        Set<UUID> vehicleIds = new HashSet<>();
        for (int index = 0; index < 101; index++) {
            vehicleIds.add(UUID.randomUUID());
        }
        var service = new TripDashboardQueryService(mock(TripDashboardRepository.class));
        assertThatThrownBy(() -> service.findActiveContexts(
                UUID.randomUUID(), vehicleIds, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100 Vehicles");
    }
}
