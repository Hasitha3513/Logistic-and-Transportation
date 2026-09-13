package com.transportlogistics.app.tracking;

import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import java.time.Instant;
import java.util.UUID;

public record VehicleRouteDeviationEscalatedV1(UUID escalationId,UUID routeDeviationEpisodeId,UUID vehicleId,UUID tripId,UUID driverId,
 UUID routeId,String routeVersion,RouteDeviationEpisode.Severity severity,DistanceMeters observedDistanceMeters,
 DistanceMeters effectiveToleranceMeters,Instant sourceTimestamp,boolean approvalRequired,EscalationReason escalationReason) {
 public enum EscalationReason { DISTANCE_HIGH,REVIEW_REJECTED }
}
