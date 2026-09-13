package com.transportlogistics.app.tracking.domain.routedeviation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteDeviationAvailabilityTest {
    @Test
    void historicalTripWithoutRouteRevisionRemainsUnknownAndTruthfullyUnavailable() {
        VehicleRouteDeviationState state = VehicleRouteDeviationState
                .unknown(UUID.randomUUID(), UUID.randomUUID())
                .unavailable(RouteDeviationAvailability.NO_ROUTE_REVISION);

        assertEquals(VehicleRouteDeviationState.State.UNKNOWN, state.state());
        assertEquals(RouteDeviationAvailability.NO_ROUTE_REVISION, state.availability());
    }
}
