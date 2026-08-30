package com.transportlogistics.app.fleet.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverPerformanceSummary(
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
        PerformanceRating overallRating,
        OffsetDateTime evaluatedAt
) {
    public static DriverPerformanceSummary calculate(
            UUID driverId,
            String driverName,
            int totalTripsAssigned,
            int totalTripsCompleted,
            int totalTripsCancelled,
            int totalViolations,
            int totalPenaltyPoints,
            int criticalViolations,
            BigDecimal totalFines,
            BigDecimal unpaidFines
    ) {
        double completionRate = totalTripsAssigned > 0
                ? (double) totalTripsCompleted / totalTripsAssigned * 100.0
                : 100.0;

        int safetyScore = Math.max(0, 100 - (totalPenaltyPoints * 4));

        PerformanceRating rating;
        if (safetyScore < 40 || criticalViolations > 1 || totalPenaltyPoints >= 12) {
            rating = PerformanceRating.AT_RISK;
        } else if (safetyScore < 60 || (unpaidFines != null && unpaidFines.compareTo(BigDecimal.ZERO) > 0) || completionRate < 70.0) {
            rating = PerformanceRating.NEEDS_IMPROVEMENT;
        } else if (safetyScore < 75 || completionRate < 85.0) {
            rating = PerformanceRating.SATISFACTORY;
        } else if (safetyScore < 90 || completionRate < 95.0) {
            rating = PerformanceRating.GOOD;
        } else {
            rating = PerformanceRating.EXCELLENT;
        }

        return new DriverPerformanceSummary(
                driverId,
                driverName,
                totalTripsAssigned,
                totalTripsCompleted,
                totalTripsCancelled,
                Math.round(completionRate * 100.0) / 100.0,
                totalViolations,
                totalPenaltyPoints,
                criticalViolations,
                totalFines != null ? totalFines : BigDecimal.ZERO,
                unpaidFines != null ? unpaidFines : BigDecimal.ZERO,
                safetyScore,
                rating,
                OffsetDateTime.now()
        );
    }
}
