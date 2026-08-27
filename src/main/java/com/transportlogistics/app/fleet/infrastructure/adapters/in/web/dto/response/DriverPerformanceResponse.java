package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverPerformanceResponse(
        UUID driverId,
        String driverName,
        int totalTripsAssigned,
        int totalTripsCompleted,
        int totalTripsCancelled,
        double tripCompletionRate,
        int totalViolations,
        int totalPenaltyPoints,
        int criticalViolations,
        BigDecimal totalFines,
        BigDecimal unpaidFines,
        int safetyScore,
        String overallRating,
        OffsetDateTime evaluatedAt
) {
}
