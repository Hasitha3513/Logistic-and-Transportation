package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceContextPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceTenantPort;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FuelPerformanceService implements FuelPerformanceQuery {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEVIATION_THRESHOLD = new BigDecimal("20.00");
    private static final BigDecimal LEAKAGE_THRESHOLD = new BigDecimal("30.00");
    private static final int MAX_SOURCE_ROWS = 100_000;
    private static final Set<String> SORTS = Set.of("consumptionRate", "adverseVariancePercent", "fuelQuantity",
            "distanceKm", "engineHours", "cost", "sampleCount");

    private final FuelIssueRepository issues;
    private final FuelPerformanceContextPort contexts;
    private final FuelPerformanceTenantPort tenants;
    private final Clock clock;

    public FuelPerformanceService(FuelIssueRepository issues, FuelPerformanceContextPort contexts,
                                  FuelPerformanceTenantPort tenants, Clock clock) {
        this.issues = issues;
        this.contexts = contexts;
        this.tenants = tenants;
        this.clock = clock;
    }

    @Override
    public FuelPerformanceSummary summary(Criteria criteria) {
        var data = load(criteria);
        var metrics = calculate(data.current(), data.baseline(), data.criteria().measurementMode(),
                data.baselinePeriod(), data.tenant().currency());
        return new FuelPerformanceSummary(data.period(), data.criteria().measurementMode(), metrics,
                distinct(data.current(), FuelIssue::vehicleId), distinctAttributedDrivers(data), now());
    }

    @Override
    public Page<VehicleFuelPerformance> vehicles(Criteria criteria, int page, int size,
                                                  String sort, String direction) {
        var data = load(criteria);
        validatePage(page, size, sort);
        var grouped = data.current().stream().collect(Collectors.groupingBy(FuelIssue::vehicleId));
        var baseline = data.baseline().stream().collect(Collectors.groupingBy(FuelIssue::vehicleId));
        var rows = grouped.entrySet().stream().map(entry -> vehicleRow(entry.getKey(), entry.getValue(),
                        baseline.getOrDefault(entry.getKey(), List.of()), data))
                .filter(Objects::nonNull).sorted(vehicleComparator(sort, direction)).toList();
        return page(rows, page, size);
    }

    @Override
    public VehicleFuelPerformance vehicle(UUID vehicleId, Criteria criteria) {
        requireId(vehicleId);
        var requested = merge(criteria, vehicleId, null);
        var data = load(requested);
        var context = data.vehicles().get(vehicleId);
        if (context == null) throw notFound("Vehicle");
        return vehicleRow(vehicleId, data.current(), data.baseline(), data);
    }

    @Override
    public Page<DriverFuelPerformance> drivers(Criteria criteria, int page, int size,
                                                String sort, String direction) {
        var data = load(criteria);
        validatePage(page, size, sort);
        var current = attributed(data.current(), data.trips());
        var previous = attributed(data.baseline(), data.trips());
        var grouped = current.stream().collect(Collectors.groupingBy(FuelIssue::driverId));
        var baseline = previous.stream().collect(Collectors.groupingBy(FuelIssue::driverId));
        var rows = grouped.entrySet().stream().map(entry -> driverRow(entry.getKey(), entry.getValue(),
                        baseline.getOrDefault(entry.getKey(), List.of()), data))
                .filter(Objects::nonNull).sorted(driverComparator(sort, direction)).toList();
        return page(rows, page, size);
    }

    @Override
    public DriverFuelPerformance driver(UUID driverId, Criteria criteria) {
        requireId(driverId);
        var data = load(merge(criteria, null, driverId));
        var context = data.drivers().get(driverId);
        if (context == null) throw notFound("Driver");
        var current = attributed(data.current(), data.trips());
        var previous = attributed(data.baseline(), data.trips());
        return driverRow(driverId, current, previous, data);
    }

    @Override
    public List<FuelPerformanceTrend> trends(Criteria criteria) {
        var data = load(criteria);
        var grain = grain(data.period());
        Map<LocalDate, List<FuelIssue>> buckets = data.current().stream().collect(Collectors.groupingBy(
                issue -> bucketStart(issue.issueDateTime().atZoneSameInstant(zone(data.tenant())).toLocalDate(), grain),
                LinkedHashMap::new, Collectors.toList()));
        var baselineMetrics = calculate(data.baseline(), List.of(), data.criteria().measurementMode(),
                null, data.tenant().currency());
        var baselineRate = baselineRate(data.baseline(), data.criteria().measurementMode());
        var results = new ArrayList<FuelPerformanceTrend>();
        for (var start : bucketStarts(data.period(), grain)) {
            var end = bucketEnd(start, grain, data.period().to());
            var metric = calculate(buckets.getOrDefault(start, List.of()), List.of(), data.criteria().measurementMode(),
                    null, data.tenant().currency());
            var change = variance(consumptionRate(buckets.getOrDefault(start, List.of()),
                    data.criteria().measurementMode()), baselineRate);
            var quality = metric.consumptionRate() == null || baselineMetrics.consumptionRate() == null
                    ? DataQuality.INSUFFICIENT
                    : metric.excludedQuantity().compareTo(BigDecimal.ZERO) > 0 || metric.unpricedCount() > 0
                    ? DataQuality.PARTIAL : DataQuality.COMPLETE;
            var indicators = change != null && change.compareTo(DEVIATION_THRESHOLD) >= 0
                    ? List.of(Indicator.EFFICIENCY_DEVIATION, Indicator.REVIEW_REQUIRED) : List.<Indicator>of();
            results.add(new FuelPerformanceTrend(start, end, grain, metric.consumptionRate(),
                    baselineMetrics.consumptionRate(), scale(change, 2), quality, indicators));
        }
        markLeakage(results);
        return List.copyOf(results);
    }

    private LoadedData load(Criteria requested) {
        var tenant = tenants.required();
        var criteria = normalize(requested);
        var range = period(criteria, tenant);
        long days = java.time.temporal.ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        var baselinePeriod = new Period(range.from().minusDays(days), range.from().minusDays(1), range.timeZone());
        var zone = zone(tenant);
        var from = baselinePeriod.from().atStartOfDay(zone).toOffsetDateTime();
        var to = range.to().plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        var source = issues.findIssuedBetween(from, to, MAX_SOURCE_ROWS + 1);
        if (source.size() > MAX_SOURCE_ROWS) {
            throw new BusinessRuleException("FUEL_PERFORMANCE_SOURCE_INVALID",
                    "Fuel performance source window exceeds the bounded query limit");
        }
        var all = source.stream().filter(issue -> matches(issue, criteria)).toList();
        var current = all.stream().filter(issue -> in(issue, range, zone)).toList();
        var baseline = all.stream().filter(issue -> in(issue, baselinePeriod, zone)).toList();
        var vehicleIds = all.stream().map(FuelIssue::vehicleId).filter(Objects::nonNull).collect(Collectors.toSet());
        var driverIds = all.stream().map(FuelIssue::driverId).filter(Objects::nonNull).collect(Collectors.toSet());
        var tripIds = all.stream().map(FuelIssue::tripId).filter(Objects::nonNull).collect(Collectors.toSet());
        var vehicles = contexts.vehicles(vehicleIds);
        var drivers = contexts.drivers(driverIds);
        var trips = contexts.trips(tripIds);
        current = current.stream().filter(issue -> compatible(issue, criteria, vehicles)).toList();
        baseline = baseline.stream().filter(issue -> compatible(issue, criteria, vehicles)).toList();
        if (criteria.vehicleId() != null && !vehicles.containsKey(criteria.vehicleId())) throw notFound("Vehicle");
        if (criteria.driverId() != null && !drivers.containsKey(criteria.driverId())) throw notFound("Driver");
        return new LoadedData(criteria, tenant, range, baselinePeriod, current, baseline,
                vehicles, drivers, trips);
    }

    private Metrics calculate(List<FuelIssue> current, List<FuelIssue> previous, MeasurementMode mode,
                              Period previousPeriod, String currency) {
        var sorted = current.stream().sorted(Comparator.comparing(FuelIssue::issueDateTime)).toList();
        var valid = sorted.stream().filter(issue -> meter(issue, mode) != null).toList();
        var excluded = sorted.stream().filter(issue -> meter(issue, mode) == null).toList();
        var reasons = new LinkedHashMap<String, Integer>();
        if (!excluded.isEmpty()) reasons.put("MISSING_DENOMINATOR", excluded.size());
        boolean invalid = descending(valid, mode);
        BigDecimal denominator = delta(valid, mode);
        BigDecimal quantity = sum(valid, FuelIssue::quantity);
        BigDecimal excludedQuantity = sum(excluded, FuelIssue::quantity);
        int priced = (int) valid.stream().filter(issue -> issue.totalAmount() != null).count();
        int unpriced = valid.size() - priced;
        BigDecimal cost = unpriced == 0 ? sum(valid, FuelIssue::totalAmount) : null;
        BigDecimal rate = positive(denominator) ? divide(quantity, denominator, 8) : null;
        BigDecimal baselineRate = baselineRate(previous, mode);
        int baselineSamples = (int) previous.stream().filter(issue -> meter(issue, mode) != null).count();
        BigDecimal adverse = baselineSamples >= 3 ? variance(rate, baselineRate) : null;
        DataQuality quality;
        if (invalid) {
            quality = DataQuality.INVALID_SOURCE_DATA;
            reasons.put("NEGATIVE_OR_RESET_UNSAFE_DELTA", 1);
            rate = null;
        } else if (!positive(denominator) || valid.isEmpty() || baselineSamples < 3 || baselineRate == null) {
            quality = DataQuality.INSUFFICIENT;
        } else if (!excluded.isEmpty() || unpriced > 0) {
            quality = DataQuality.PARTIAL;
        } else {
            quality = DataQuality.COMPLETE;
        }
        var indicators = adverse != null && adverse.compareTo(DEVIATION_THRESHOLD) >= 0
                ? List.of(Indicator.EFFICIENCY_DEVIATION, Indicator.REVIEW_REQUIRED) : List.<Indicator>of();
        var baseline = new Baseline("SAME_VEHICLE_PRECEDING_EQUAL_WINDOW", previousPeriod,
                baselineSamples, scale(baselineRate, 3));
        BigDecimal lpk = mode == MeasurementMode.DISTANCE ? scale(rate, 3) : null;
        BigDecimal l100 = lpk == null ? null : scale(lpk.multiply(HUNDRED), 3);
        BigDecimal kmpl = mode == MeasurementMode.DISTANCE && positive(quantity) && positive(denominator)
                ? divide(denominator, quantity, 3) : null;
        BigDecimal lph = mode == MeasurementMode.ENGINE_HOURS ? scale(rate, 3) : null;
        BigDecimal costPer = cost != null && positive(denominator) ? divide(cost, denominator, 2) : null;
        return new Metrics(scale(quantity, 3), mode == MeasurementMode.DISTANCE ? scale(denominator, 3) : null,
                mode == MeasurementMode.ENGINE_HOURS ? scale(denominator, 3) : null, lpk, l100, kmpl, lph,
                scale(cost, 2), mode == MeasurementMode.DISTANCE ? costPer : null,
                mode == MeasurementMode.ENGINE_HOURS ? costPer : null, valid.size(), priced, unpriced,
                scale(quantity, 3), scale(excludedQuantity, 3), scale(rate, 3), scale(adverse, 2), quality,
                Map.copyOf(reasons), baseline, indicators, cost == null ? null : currency);
    }

    private VehicleFuelPerformance vehicleRow(UUID id, List<FuelIssue> current, List<FuelIssue> previous,
                                               LoadedData data) {
        var context = data.vehicles().get(id);
        if (context == null || !context.active()) return null;
        var metrics = calculate(current, previous, data.criteria().measurementMode(),
                data.baselinePeriod(), data.tenant().currency());
        BigDecimal peer = peerRate(id, data, context.typeId());
        String fuelType = current.stream().map(FuelIssue::fuelType).findFirst().orElse(data.criteria().fuelType());
        return new VehicleFuelPerformance(id, context.label(), context.typeId(), fuelType,
                data.criteria().measurementMode(), metrics, peer, now());
    }

    private DriverFuelPerformance driverRow(UUID id, List<FuelIssue> current, List<FuelIssue> previous,
                                             LoadedData data) {
        var context = data.drivers().get(id);
        if (context == null || !context.active()) return null;
        var metrics = calculate(current, previous, data.criteria().measurementMode(),
                data.baselinePeriod(), data.tenant().currency());
        String fuelType = current.stream().map(FuelIssue::fuelType).findFirst().orElse(data.criteria().fuelType());
        return new DriverFuelPerformance(id, context.label(), fuelType, data.criteria().measurementMode(),
                metrics, now());
    }

    private BigDecimal peerRate(UUID vehicleId, LoadedData data, UUID typeId) {
        var groups = data.current().stream().filter(issue -> {
            var value = data.vehicles().get(issue.vehicleId());
            return value != null && Objects.equals(value.typeId(), typeId) && !issue.vehicleId().equals(vehicleId);
        }).collect(Collectors.groupingBy(FuelIssue::vehicleId));
        var rates = groups.values().stream().map(values -> calculate(values, List.of(),
                        data.criteria().measurementMode(), null, data.tenant().currency())
                        .consumptionRate()).filter(Objects::nonNull).toList();
        if (groups.size() + 1 < 3 || rates.isEmpty()) return null;
        return divide(rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add), BigDecimal.valueOf(rates.size()), 3);
    }

    private static List<FuelIssue> attributed(List<FuelIssue> values,
                                               Map<UUID, FuelPerformanceContextPort.TripContext> trips) {
        return values.stream().filter(issue -> {
            if (issue.driverId() == null) return false;
            if (issue.tripId() == null) return true;
            var trip = trips.get(issue.tripId());
            return trip != null && Objects.equals(trip.driverId(), issue.driverId())
                    && Objects.equals(trip.vehicleId(), issue.vehicleId());
        }).toList();
    }

    private static boolean matches(FuelIssue issue, Criteria criteria) {
        return (criteria.vehicleId() == null || criteria.vehicleId().equals(issue.vehicleId()))
                && (criteria.driverId() == null || criteria.driverId().equals(issue.driverId()))
                && (criteria.fuelType() == null || criteria.fuelType().equalsIgnoreCase(issue.fuelType()));
    }

    private static boolean compatible(FuelIssue issue, Criteria criteria,
                                      Map<UUID, FuelPerformanceContextPort.VehicleContext> vehicles) {
        var vehicle = vehicles.get(issue.vehicleId());
        return vehicle != null && vehicle.active()
                && (criteria.vehicleTypeId() == null || criteria.vehicleTypeId().equals(vehicle.typeId()));
    }

    private Criteria normalize(Criteria value) {
        var criteria = value == null ? new Criteria(null, null, null, null, null, null, null, null) : value;
        var mode = criteria.measurementMode() == null ? MeasurementMode.DISTANCE : criteria.measurementMode();
        String fuelType = criteria.fuelType() == null || criteria.fuelType().isBlank()
                ? null : criteria.fuelType().trim().toUpperCase();
        return new Criteria(criteria.preset(), criteria.from(), criteria.to(), criteria.vehicleId(),
                criteria.driverId(), criteria.vehicleTypeId(), fuelType, mode);
    }

    private Period period(Criteria criteria, FuelPerformanceTenantPort.TenantContext tenant) {
        var today = LocalDate.now(clock.withZone(zone(tenant)));
        if (criteria.from() != null || criteria.to() != null) {
            if (criteria.from() == null || criteria.to() == null || criteria.from().isAfter(criteria.to())
                    || criteria.to().isAfter(today)
                    || java.time.temporal.ChronoUnit.DAYS.between(criteria.from(), criteria.to()) + 1 > 365) {
                throw invalidRange();
            }
            return new Period(criteria.from(), criteria.to(), tenant.timeZone());
        }
        int days = criteria.preset() == null ? 30 : criteria.preset();
        if (days != 7 && days != 30 && days != 90) throw invalidRange();
        return new Period(today.minusDays(days - 1L), today, tenant.timeZone());
    }

    private static BigDecimal meter(FuelIssue issue, MeasurementMode mode) {
        return mode == MeasurementMode.DISTANCE ? issue.odometer() : issue.engineHours();
    }

    private static boolean descending(List<FuelIssue> values, MeasurementMode mode) {
        return values.stream().collect(Collectors.groupingBy(FuelIssue::vehicleId)).values().stream()
                .anyMatch(vehicleValues -> descendingForVehicle(vehicleValues, mode));
    }

    private static BigDecimal delta(List<FuelIssue> values, MeasurementMode mode) {
        var deltas = values.stream().collect(Collectors.groupingBy(FuelIssue::vehicleId)).values().stream()
                .map(vehicleValues -> deltaForVehicle(vehicleValues, mode))
                .filter(Objects::nonNull)
                .toList();
        return deltas.isEmpty() ? null : deltas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean descendingForVehicle(List<FuelIssue> values, MeasurementMode mode) {
        var sorted = values.stream().sorted(Comparator.comparing(FuelIssue::issueDateTime)).toList();
        for (int index = 1; index < sorted.size(); index++) {
            if (meter(sorted.get(index), mode).compareTo(meter(sorted.get(index - 1), mode)) < 0) return true;
        }
        return false;
    }

    private static BigDecimal deltaForVehicle(List<FuelIssue> values, MeasurementMode mode) {
        if (values.size() < 2) return null;
        var sorted = values.stream().sorted(Comparator.comparing(FuelIssue::issueDateTime)).toList();
        return meter(sorted.get(sorted.size() - 1), mode).subtract(meter(sorted.get(0), mode));
    }

    private static BigDecimal baselineRate(List<FuelIssue> values, MeasurementMode mode) {
        var valid = values.stream().filter(issue -> meter(issue, mode) != null)
                .sorted(Comparator.comparing(FuelIssue::issueDateTime)).toList();
        var denominator = delta(valid, mode);
        return valid.size() >= 3 && positive(denominator) ? divide(sum(valid, FuelIssue::quantity), denominator, 8) : null;
    }

    private static BigDecimal consumptionRate(List<FuelIssue> values, MeasurementMode mode) {
        var valid = values.stream().filter(issue -> meter(issue, mode) != null).toList();
        var denominator = delta(valid, mode);
        return !descending(valid, mode) && positive(denominator)
                ? divide(sum(valid, FuelIssue::quantity), denominator, 8) : null;
    }

    private static BigDecimal variance(BigDecimal current, BigDecimal baseline) {
        return current == null || !positive(baseline) ? null
                : current.subtract(baseline).divide(baseline, 8, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private static <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> getter) {
        return values.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal divide(BigDecimal left, BigDecimal right, int scale) {
        return left.divide(right, scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private static boolean in(FuelIssue issue, Period period, ZoneId zone) {
        var date = issue.issueDateTime().atZoneSameInstant(zone).toLocalDate();
        return !date.isBefore(period.from()) && !date.isAfter(period.to());
    }

    private static TrendGrain grain(Period period) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(period.from(), period.to()) + 1;
        return days <= 30 ? TrendGrain.DAILY : days <= 90 ? TrendGrain.WEEKLY : TrendGrain.MONTHLY;
    }

    private static LocalDate bucketStart(LocalDate date, TrendGrain grain) {
        return switch (grain) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    private static LocalDate bucketEnd(LocalDate start, TrendGrain grain, LocalDate periodEnd) {
        var end = switch (grain) {
            case DAILY -> start;
            case WEEKLY -> start.plusDays(6);
            case MONTHLY -> start.with(TemporalAdjusters.lastDayOfMonth());
        };
        return end.isAfter(periodEnd) ? periodEnd : end;
    }

    private static List<LocalDate> bucketStarts(Period period, TrendGrain grain) {
        var starts = new ArrayList<LocalDate>();
        var cursor = period.from();
        LocalDate previous = null;
        while (!cursor.isAfter(period.to())) {
            var start = bucketStart(cursor, grain);
            if (!start.equals(previous)) starts.add(start);
            previous = start;
            cursor = cursor.plusDays(1);
        }
        return List.copyOf(starts);
    }

    private static void markLeakage(List<FuelPerformanceTrend> values) {
        for (int index = 1; index < values.size(); index++) {
            var previous = values.get(index - 1);
            var current = values.get(index);
            if (qualifies(previous) && qualifies(current)) {
                var indicators = new ArrayList<>(current.indicators());
                indicators.add(Indicator.POSSIBLE_LEAKAGE_INDICATOR);
                values.set(index, new FuelPerformanceTrend(current.bucketStart(), current.bucketEnd(), current.grain(),
                        current.actualRate(), current.baselineRate(), current.percentChange(), current.quality(),
                        List.copyOf(indicators)));
            }
        }
    }

    private static boolean qualifies(FuelPerformanceTrend value) {
        return value.quality() != DataQuality.INSUFFICIENT && value.quality() != DataQuality.INVALID_SOURCE_DATA
                && value.percentChange() != null && value.percentChange().compareTo(LEAKAGE_THRESHOLD) >= 0;
    }

    private static Comparator<VehicleFuelPerformance> vehicleComparator(String sort, String direction) {
        Comparator<VehicleFuelPerformance> comparator = Comparator.comparing(row -> sortable(row.metrics(), sort),
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!"asc".equalsIgnoreCase(direction)) comparator = comparator.reversed();
        return comparator.thenComparing(VehicleFuelPerformance::vehicleId);
    }

    private static Comparator<DriverFuelPerformance> driverComparator(String sort, String direction) {
        Comparator<DriverFuelPerformance> comparator = Comparator.comparing(row -> sortable(row.metrics(), sort),
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!"asc".equalsIgnoreCase(direction)) comparator = comparator.reversed();
        return comparator.thenComparing(DriverFuelPerformance::driverId);
    }

    private static BigDecimal sortable(Metrics metrics, String sort) {
        return switch (sort == null ? "adverseVariancePercent" : sort) {
            case "consumptionRate" -> metrics.consumptionRate();
            case "fuelQuantity" -> metrics.consumedLitres();
            case "distanceKm" -> metrics.distanceKm();
            case "engineHours" -> metrics.engineHours();
            case "cost" -> metrics.totalCost();
            case "sampleCount" -> BigDecimal.valueOf(metrics.sampleCount());
            default -> metrics.adverseVariancePercent();
        };
    }

    private static void validatePage(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || (sort != null && !SORTS.contains(sort))) {
            throw new BusinessRuleException("FUEL_PERFORMANCE_INVALID_RANGE", "Invalid pagination or sort");
        }
    }

    private static <T> Page<T> page(List<T> values, int page, int size) {
        int start = Math.min(page * size, values.size());
        int end = Math.min(start + size, values.size());
        int pages = values.isEmpty() ? 0 : (values.size() + size - 1) / size;
        return new Page<>(values.subList(start, end), page, size, values.size(), pages);
    }

    private static int distinct(List<FuelIssue> values, Function<FuelIssue, UUID> getter) {
        return (int) values.stream().map(getter).filter(Objects::nonNull).distinct().count();
    }

    private static int distinctAttributedDrivers(LoadedData data) {
        return distinct(attributed(data.current(), data.trips()), FuelIssue::driverId);
    }

    private static Criteria merge(Criteria criteria, UUID vehicleId, UUID driverId) {
        var value = criteria == null ? new Criteria(null, null, null, null, null, null, null, null) : criteria;
        return new Criteria(value.preset(), value.from(), value.to(), vehicleId == null ? value.vehicleId() : vehicleId,
                driverId == null ? value.driverId() : driverId, value.vehicleTypeId(), value.fuelType(),
                value.measurementMode());
    }

    private static void requireId(UUID id) {
        if (id == null) throw notFound("Resource");
    }

    private static NotFoundException notFound(String type) {
        return new NotFoundException("FUEL_PERFORMANCE_NOT_FOUND", type + " performance not found");
    }

    private static BusinessRuleException invalidRange() {
        return new BusinessRuleException("FUEL_PERFORMANCE_INVALID_RANGE", "Invalid fuel performance date range");
    }

    private ZoneId zone(FuelPerformanceTenantPort.TenantContext tenant) {
        try {
            return ZoneId.of(tenant.timeZone());
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("FUEL_PERFORMANCE_SOURCE_INVALID", "Tenant timezone is invalid");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private record LoadedData(Criteria criteria, FuelPerformanceTenantPort.TenantContext tenant, Period period,
                              Period baselinePeriod, List<FuelIssue> current, List<FuelIssue> baseline,
                              Map<UUID, FuelPerformanceContextPort.VehicleContext> vehicles,
                              Map<UUID, FuelPerformanceContextPort.DriverContext> drivers,
                              Map<UUID, FuelPerformanceContextPort.TripContext> trips) {}
}
