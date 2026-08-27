package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriverPerformanceSummaryTest {

    @Test
    void calculatesScorecardForPristineDriver() {
        var driverId = UUID.randomUUID();
        var summary = DriverPerformanceSummary.calculate(
                driverId,
                "John Doe",
                20,
                20,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertEquals(100, summary.safetyScore());
        assertEquals(100.0, summary.tripCompletionRate());
        assertEquals(PerformanceRating.EXCELLENT, summary.overallRating());
    }

    @Test
    void calculatesScorecardForDriverWithModeratePenalties() {
        var driverId = UUID.randomUUID();
        var summary = DriverPerformanceSummary.calculate(
                driverId,
                "Jane Smith",
                10,
                9,
                1,
                2,
                5, // 5 penalty points -> 100 - 20 = 80 safety score
                0,
                new BigDecimal("300.00"),
                BigDecimal.ZERO
        );

        assertEquals(80, summary.safetyScore());
        assertEquals(90.0, summary.tripCompletionRate());
        assertEquals(PerformanceRating.GOOD, summary.overallRating());
    }

    @Test
    void flagsAtRiskDriverForExcessivePointsOrCriticalViolations() {
        var driverId = UUID.randomUUID();
        var summary = DriverPerformanceSummary.calculate(
                driverId,
                "Risky Driver",
                15,
                10,
                5,
                4,
                16, // 16 penalty points -> safety score 36 (<40) & points >= 12
                2,  // 2 critical violations
                new BigDecimal("1500.00"),
                new BigDecimal("500.00")
        );

        assertEquals(36, summary.safetyScore());
        assertEquals(66.67, summary.tripCompletionRate());
        assertEquals(PerformanceRating.AT_RISK, summary.overallRating());
    }
}
