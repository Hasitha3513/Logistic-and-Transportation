package com.transportlogistics.app.tracking.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeofencePolygonTest {
    @Test
    void acceptsTriangleAndExposesCanonicalRingAndBounds() {
        GeofencePolygon polygon = polygon();

        assertThat(polygon.vertices()).hasSize(3);
        assertThat(polygon.closedRing()).hasSize(4);
        assertThat(polygon.closedRing().getFirst()).isEqualTo(polygon.closedRing().getLast());
        assertThat(polygon.boundingBox()).isEqualTo(new BoundingBox(0, 0, 10, 10));
    }

    @Test
    void pointInPolygonTreatsInteriorEdgesAndVerticesAsInside() {
        GeofencePolygon polygon = polygon();

        assertThat(polygon.contains(point(2, 2))).isTrue();
        assertThat(polygon.contains(point(5, 0))).isTrue();
        assertThat(polygon.contains(point(0, 0))).isTrue();
        assertThat(polygon.contains(point(11, 2))).isFalse();
        assertThat(polygon.contains(point(-1, -1))).isFalse();
    }

    @Test
    void orientationDoesNotChangeContainment() {
        List<Wgs84Coordinate> reversed = new ArrayList<>(polygon().vertices());
        Collections.reverse(reversed);

        assertThat(GeofencePolygon.of(reversed).contains(point(2, 2))).isTrue();
        assertThat(GeofencePolygon.of(reversed).vertices()).containsExactlyElementsOf(reversed);
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThatThrownBy(() -> point(181, 0)).isInstanceOf(GeofenceRuleException.class);
        assertThatThrownBy(() -> point(0, -91)).isInstanceOf(GeofenceRuleException.class);
        assertThatThrownBy(() -> point(Double.NaN, 0)).isInstanceOf(GeofenceRuleException.class);
        assertThatThrownBy(() -> point(0, Double.POSITIVE_INFINITY))
                .isInstanceOf(GeofenceRuleException.class);
    }

    @Test
    void rejectsTooFewAndTooManyVertices() {
        assertInvalid(List.of(point(0, 0), point(1, 1)));
        List<Wgs84Coordinate> vertices = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            double angle = 2 * Math.PI * index / 101;
            vertices.add(point(Math.cos(angle), Math.sin(angle)));
        }
        assertInvalid(vertices);
    }

    @Test
    void rejectsDuplicateAndNullVertices() {
        assertInvalid(List.of(point(0, 0), point(10, 0), point(10, 0), point(0, 10)));
        List<Wgs84Coordinate> vertices = new ArrayList<>(List.of(point(0, 0), point(10, 0), point(0, 10)));
        vertices.set(1, null);
        assertInvalid(vertices);
    }

    @Test
    void rejectsSelfIntersectionAndNonAdjacentTouch() {
        assertInvalid(List.of(point(0, 0), point(10, 10), point(0, 10), point(10, 0)));
        assertInvalid(List.of(point(0, 0), point(10, 0), point(5, 5), point(0, 10), point(5, 5)));
    }

    @Test
    void rejectsCollinearAndEffectiveZeroArea() {
        assertInvalid(List.of(point(0, 0), point(1, 1), point(2, 2)));
        assertInvalid(List.of(point(0, 0), point(1, 1e-14), point(2, 0)));
    }

    private static GeofencePolygon polygon() {
        return GeofencePolygon.of(List.of(point(0, 0), point(10, 0), point(0, 10)));
    }

    private static Wgs84Coordinate point(double longitude, double latitude) {
        return new Wgs84Coordinate(longitude, latitude);
    }

    private static void assertInvalid(List<Wgs84Coordinate> vertices) {
        assertThatThrownBy(() -> GeofencePolygon.of(vertices))
                .isInstanceOf(GeofenceRuleException.class)
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_INVALID_GEOMETRY");
    }
}
