package com.transportlogistics.app.routing.application.service;

import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class RouteServiceTest {
    private RouteRepository repository;
    private RouteService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteRepository.class);
        service = new RouteService(repository);
    }

    @Test
    void delegatesFilteredSearchWithTrimmedQuery() {
        var origin = UUID.randomUUID();
        service.search("  central  ", origin, null, true);
        verify(repository).search("central", origin, null, true);
    }

    @Test
    void unfilteredSearchPreservesExistingListCapability() {
        service.search(" ", null, null, null);
        verify(repository).findAll();
        verify(repository, never()).search(any(), any(), any(), any());
    }

    @Test
    void deactivationPreservesStopsAndRoutePlanningData() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(java.util.Optional.of(route));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(route.id());

        verify(repository).save(argThat(saved -> {
            assertFalse(saved.active());
            return saved.stopLocationIds().equals(route.stopLocationIds())
                    && saved.plannedDistanceKm().equals(route.plannedDistanceKm())
                    && saved.estimatedDurationMinutes().equals(route.estimatedDurationMinutes());
        }));
    }

    private Route route() {
        return new Route(UUID.randomUUID(), "RT-1", "Central route", UUID.randomUUID(), UUID.randomUUID(),
                50.0, 75, true, List.of(UUID.randomUUID()));
    }
}
