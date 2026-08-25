package com.transportlogistics.app.routing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
/**
 * Outbound SPI exposed by routing and implemented by the trip module.
 * Preserves strict unidirectional dependency (trip -> routing).
 */
public interface RoutePerformanceTripLookupPort {

    List<RouteTripMetric> findTripsForRoute(UUID routeId, OffsetDateTime from, OffsetDateTime to);

}
