package com.transportlogistics.app.tracking.ports.outbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface RouteDeviationEventPublisherPort {
    void publishDetected(UUID tenantId, Detected event);
    void publishEscalated(UUID tenantId, Escalated event);

    record Detected(UUID episodeId, UUID vehicleId, UUID tripId, UUID driverId, UUID routeId,
                    String routeVersion, String severity, BigDecimal observedDistanceMeters,
                    BigDecimal effectiveToleranceMeters, Instant sourceTimestamp,
                    boolean approvalRequired) { }

    record Escalated(UUID eventId, UUID episodeId, UUID vehicleId, UUID tripId, UUID driverId,
                     UUID routeId, String routeVersion, String severity,
                     BigDecimal observedDistanceMeters, BigDecimal effectiveToleranceMeters,
                     Instant sourceTimestamp, boolean approvalRequired, String escalationReason) { }
}
