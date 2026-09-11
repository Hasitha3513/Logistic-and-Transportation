package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FuelPerformanceQuery {
    FuelPerformanceSummary summary(Criteria criteria);
    Page<VehicleFuelPerformance> vehicles(Criteria criteria, int page, int size, String sort, String direction);
    VehicleFuelPerformance vehicle(UUID vehicleId, Criteria criteria);
    Page<DriverFuelPerformance> drivers(Criteria criteria, int page, int size, String sort, String direction);
    DriverFuelPerformance driver(UUID driverId, Criteria criteria);
    List<FuelPerformanceTrend> trends(Criteria criteria);

    enum MeasurementMode { DISTANCE, ENGINE_HOURS }
    enum DataQuality { COMPLETE, PARTIAL, INSUFFICIENT, INVALID_SOURCE_DATA }
    enum Indicator { EFFICIENCY_DEVIATION, POSSIBLE_LEAKAGE_INDICATOR, REVIEW_REQUIRED }
    enum TrendGrain { DAILY, WEEKLY, MONTHLY }

    record Criteria(Integer preset, LocalDate from, LocalDate to, UUID vehicleId, UUID driverId,
                    UUID vehicleTypeId, String fuelType, MeasurementMode measurementMode) {}

    record Period(LocalDate from, LocalDate to, String timeZone) {}
    record Baseline(String type, Period period, int sampleCount, BigDecimal rate) {}
    record Metrics(BigDecimal consumedLitres, BigDecimal distanceKm, BigDecimal engineHours,
                   BigDecimal litresPerKm, BigDecimal litresPer100Km, BigDecimal kmPerLitre,
                   BigDecimal litresPerEngineHour, BigDecimal totalCost, BigDecimal costPerKm,
                   BigDecimal costPerEngineHour, int sampleCount, int pricedCount, int unpricedCount,
                   BigDecimal validQuantity, BigDecimal excludedQuantity, BigDecimal consumptionRate,
                   BigDecimal adverseVariancePercent, DataQuality quality, Map<String, Integer> exclusionReasons,
                   Baseline baseline, List<Indicator> indicators, String currency) {}
    record FuelPerformanceSummary(Period period, MeasurementMode measurementMode, Metrics metrics,
                                  int vehicleCount, int driverCount, OffsetDateTime calculatedAt) {}
    record VehicleFuelPerformance(UUID vehicleId, String vehicleLabel, UUID vehicleTypeId, String fuelType,
                                  MeasurementMode measurementMode, Metrics metrics, BigDecimal peerRate,
                                  OffsetDateTime calculatedAt) {}
    record DriverFuelPerformance(UUID driverId, String driverLabel, String fuelType,
                                 MeasurementMode measurementMode, Metrics metrics, OffsetDateTime calculatedAt) {}
    record FuelPerformanceTrend(LocalDate bucketStart, LocalDate bucketEnd, TrendGrain grain,
                                BigDecimal actualRate, BigDecimal baselineRate, BigDecimal percentChange,
                                DataQuality quality, List<Indicator> indicators) {}
    record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
