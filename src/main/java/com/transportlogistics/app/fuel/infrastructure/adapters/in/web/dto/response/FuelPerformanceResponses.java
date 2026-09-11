package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FuelPerformanceResponses {
    private FuelPerformanceResponses() {}

    public record Period(LocalDate from, LocalDate to, String timeZone) {}
    public record Baseline(String type, Period period, int sampleCount, BigDecimal rate) {}
    public record Metrics(BigDecimal consumedLitres, BigDecimal distanceKm, BigDecimal engineHours,
                          BigDecimal litresPerKm, BigDecimal litresPer100Km, BigDecimal kmPerLitre,
                          BigDecimal litresPerEngineHour, BigDecimal totalCost, BigDecimal costPerKm,
                          BigDecimal costPerEngineHour, int sampleCount, int pricedCount, int unpricedCount,
                          BigDecimal validQuantity, BigDecimal excludedQuantity, BigDecimal consumptionRate,
                          BigDecimal adverseVariancePercent, FuelPerformanceQuery.DataQuality quality,
                          Map<String, Integer> exclusionReasons, Baseline baseline,
                          List<FuelPerformanceQuery.Indicator> indicators, String currency) {}
    public record Summary(Period period, FuelPerformanceQuery.MeasurementMode measurementMode, Metrics metrics,
                          int vehicleCount, int driverCount, OffsetDateTime calculatedAt) {}
    public record Vehicle(UUID vehicleId, String vehicleLabel, UUID vehicleTypeId, String fuelType,
                          FuelPerformanceQuery.MeasurementMode measurementMode, Metrics metrics,
                          BigDecimal peerRate, OffsetDateTime calculatedAt) {}
    public record Driver(UUID driverId, String driverLabel, String fuelType,
                         FuelPerformanceQuery.MeasurementMode measurementMode, Metrics metrics,
                         OffsetDateTime calculatedAt) {}
    public record Trend(LocalDate bucketStart, LocalDate bucketEnd, FuelPerformanceQuery.TrendGrain grain,
                        BigDecimal actualRate, BigDecimal baselineRate, BigDecimal percentChange,
                        FuelPerformanceQuery.DataQuality quality,
                        List<FuelPerformanceQuery.Indicator> indicators) {}
    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
