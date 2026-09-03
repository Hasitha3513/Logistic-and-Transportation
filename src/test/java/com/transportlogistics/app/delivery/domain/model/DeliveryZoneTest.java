package com.transportlogistics.app.delivery.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryZoneTest {

    private final UUID tenantId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Successfully creates valid DeliveryZone with closed polygon")
    void createValidZone() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8450, 6.9271),
                new DeliveryZoneCoordinate(79.8600, 6.9271),
                new DeliveryZoneCoordinate(79.8600, 6.9400),
                new DeliveryZoneCoordinate(79.8450, 6.9400),
                new DeliveryZoneCoordinate(79.8450, 6.9271)
        );
        DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(coords);

        DeliveryZone zone = DeliveryZone.create(
                tenantId,
                "zone-col-01",
                "Colombo Central",
                "Commercial area",
                DeliveryZoneType.URBAN_DENSE,
                true,
                100,
                null,
                boundary,
                10,
                "admin",
                now
        );

        assertThat(zone.zoneCode()).isEqualTo("ZONE-COL-01");
        assertThat(zone.status()).isEqualTo(DeliveryZoneStatus.ACTIVE);
        assertThat(zone.serviceable()).isTrue();
        assertThat(zone.priority()).isEqualTo(10);
        assertThat(zone.boundary().boundingBox().minLatitude()).isEqualTo(6.9271);
        assertThat(zone.boundary().boundingBox().maxLatitude()).isEqualTo(6.9400);
        assertThat(zone.boundary().approximateArea()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Rejects unclosed polygon ring")
    void rejectUnclosedPolygon() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8450, 6.9271),
                new DeliveryZoneCoordinate(79.8600, 6.9271),
                new DeliveryZoneCoordinate(79.8600, 6.9400),
                new DeliveryZoneCoordinate(79.8450, 6.9400)
        );

        assertThatThrownBy(() -> new DeliveryZoneBoundary(coords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Polygon ring must be closed");
    }

    @Test
    @DisplayName("Point-in-polygon correctly identifies inside, outside, and boundary points")
    void pointInPolygonContainment() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(coords);

        // Inside point
        assertThat(boundary.contains(15.0, 15.0)).isTrue();

        // Outside point
        assertThat(boundary.contains(25.0, 15.0)).isFalse();
        assertThat(boundary.contains(5.0, 15.0)).isFalse();

        // Boundary point (edge)
        assertThat(boundary.contains(10.0, 15.0)).isTrue();
        // Boundary point (vertex)
        assertThat(boundary.contains(20.0, 20.0)).isTrue();
    }

    @Test
    @DisplayName("Supports activation, deactivation, and updates")
    void lifecycleAndUpdates() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(0.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 0.0)
        );
        DeliveryZone zone = DeliveryZone.create(
                tenantId,
                "ZONE-1",
                "Zone 1",
                "Desc",
                DeliveryZoneType.SUBURBAN,
                true,
                50,
                null,
                new DeliveryZoneBoundary(coords),
                5,
                "admin",
                now
        );

        zone.deactivate("admin", now.plusHours(1));
        assertThat(zone.status()).isEqualTo(DeliveryZoneStatus.INACTIVE);

        zone.activate("admin", now.plusHours(2));
        assertThat(zone.status()).isEqualTo(DeliveryZoneStatus.ACTIVE);
    }
}
