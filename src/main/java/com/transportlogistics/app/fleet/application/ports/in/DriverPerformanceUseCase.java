package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverPerformanceSummary;

import java.util.UUID;

public interface DriverPerformanceUseCase {
    DriverPerformanceSummary getPerformanceSummary(UUID driverId);
}
