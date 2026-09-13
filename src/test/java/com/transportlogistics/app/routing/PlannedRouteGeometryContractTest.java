package com.transportlogistics.app.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlannedRouteGeometryContractTest {
    @Test
    void preservesExactRevisionCoordinateOrderPrecisionAndImmutability() {
        UUID routeId = UUID.randomUUID();
        List<Wgs84Point> source = new ArrayList<>(List.of(
                point("79.86124319", "6.92707842"),
                point("79.86124419", "6.92707842")));
        PlannedRouteGeometry geometry = new PlannedRouteGeometry(routeId, "REVISION:27", source);
        source.clear();

        assertEquals(routeId, geometry.routeId());
        assertEquals("REVISION:27", geometry.routeVersion());
        assertEquals(new BigDecimal("79.86124319"), geometry.orderedPoints().get(0).longitude());
        assertEquals(new BigDecimal("6.92707842"), geometry.orderedPoints().get(0).latitude());
        assertEquals(2, geometry.orderedPoints().size());
        assertThrows(UnsupportedOperationException.class,
                () -> geometry.orderedPoints().add(point("79.86124519", "6.92707842")));
    }

    @Test
    void validatesPointCountCoordinatesAndAdjacentUniqueness() {
        UUID routeId = UUID.randomUUID();
        Wgs84Point point = point("79.86", "6.92");
        assertThrows(IllegalArgumentException.class,
                () -> new PlannedRouteGeometry(routeId, "REVISION:1", List.of(point)));
        List<Wgs84Point> tooMany = new ArrayList<>();
        for (int index = 0; index < 2_001; index++) {
            tooMany.add(point(BigDecimal.valueOf(index).movePointLeft(6).toPlainString(), "0"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new PlannedRouteGeometry(routeId, "REVISION:1", tooMany));
        assertThrows(IllegalArgumentException.class,
                () -> new PlannedRouteGeometry(routeId, "REVISION:1", List.of(point, point)));
        assertThrows(IllegalArgumentException.class, () -> point("180.000001", "0"));
        assertThrows(IllegalArgumentException.class, () -> point("0", "90.000001"));
    }

    @Test
    void rejectsAntimeridianCrossingAndSegmentsLongerThanTwentyFiveKilometres() {
        UUID routeId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new PlannedRouteGeometry(routeId,
                "REVISION:1", List.of(point("179.9", "0"), point("-179.9", "0"))));
        assertThrows(IllegalArgumentException.class, () -> new PlannedRouteGeometry(routeId,
                "REVISION:1", List.of(point("0", "0"), point("0.3", "0"))));
    }

    @Test
    void requiresCanonicalExactRevision() {
        UUID routeId = UUID.randomUUID();
        List<Wgs84Point> points = List.of(point("0", "0"), point("0.000001", "0"));
        assertThrows(IllegalArgumentException.class,
                () -> new PlannedRouteGeometry(routeId, "LATEST", points));
        assertThrows(IllegalArgumentException.class,
                () -> new PlannedRouteGeometry(routeId, "REVISION:0", points));
    }

    private static Wgs84Point point(String longitude, String latitude) {
        return new Wgs84Point(new BigDecimal(longitude), new BigDecimal(latitude));
    }
}
