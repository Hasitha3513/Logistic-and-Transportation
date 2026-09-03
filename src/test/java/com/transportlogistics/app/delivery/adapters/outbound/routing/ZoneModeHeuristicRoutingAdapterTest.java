package com.transportlogistics.app.delivery.adapters.outbound.routing;

import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.domain.model.EtaSource;
import com.transportlogistics.app.delivery.ports.outbound.LastMileRoutingPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZoneModeHeuristicRoutingAdapterTest {

    private final ZoneModeHeuristicRoutingAdapter adapter = new ZoneModeHeuristicRoutingAdapter();

    @Test
    @DisplayName("Should calculate Haversine distance and duration accurately for urban motorbike")
    void estimate_urbanDense_motorbike_succeeds() {
        // Colombo Galle Face (6.9271, 79.8436) to Bambalapitiya (6.8920, 79.8550) ~4.1 km straight line
        LastMileRoutingPort.Coordinate origin = new LastMileRoutingPort.Coordinate(6.9271, 79.8436);
        LastMileRoutingPort.Coordinate destination = new LastMileRoutingPort.Coordinate(6.8920, 79.8550);

        LastMileRoutingPort.RouteEstimate estimate = adapter.estimate(
                origin,
                destination,
                DeliveryTransportMode.MOTORBIKE,
                DeliveryZoneType.URBAN_DENSE,
                OffsetDateTime.now()
        );

        assertThat(estimate.source()).isEqualTo(EtaSource.HEURISTIC);
        assertThat(estimate.distanceMeters()).isBetween(4500L, 6500L); // ~5.3 km with 1.3 circuity factor
        // Motorbike speed in URBAN_DENSE is 25 km/h -> ~6.94 m/s. ~5300m / 6.94 = ~760s (~12.6m)
        assertThat(estimate.durationSeconds()).isBetween(600L, 900L);
    }

    @Test
    @DisplayName("Should vary duration based on transport mode")
    void estimate_modeDifferences_correct() {
        LastMileRoutingPort.Coordinate origin = new LastMileRoutingPort.Coordinate(6.9271, 79.8436);
        LastMileRoutingPort.Coordinate destination = new LastMileRoutingPort.Coordinate(6.8920, 79.8550);
        OffsetDateTime now = OffsetDateTime.now();

        LastMileRoutingPort.RouteEstimate walker = adapter.estimate(origin, destination, DeliveryTransportMode.WALKER, DeliveryZoneType.URBAN_DENSE, now);
        LastMileRoutingPort.RouteEstimate bicycle = adapter.estimate(origin, destination, DeliveryTransportMode.BICYCLE, DeliveryZoneType.URBAN_DENSE, now);
        LastMileRoutingPort.RouteEstimate motorbike = adapter.estimate(origin, destination, DeliveryTransportMode.MOTORBIKE, DeliveryZoneType.URBAN_DENSE, now);

        assertThat(walker.durationSeconds()).isGreaterThan(bicycle.durationSeconds());
        assertThat(bicycle.durationSeconds()).isGreaterThan(motorbike.durationSeconds());
    }

    @Test
    @DisplayName("Should reject invalid coordinates")
    void estimate_invalidCoordinates_throws() {
        assertThatThrownBy(() -> new LastMileRoutingPort.Coordinate(100.0, 50.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
