package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceContextPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceTenantPort;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelPerformanceServiceTest {
    private static final UUID VEHICLE = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TYPE = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID DRIVER = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID TRIP = UUID.fromString("00000000-0000-0000-0000-000000000104");
    private final InMemoryIssues repository = new InMemoryIssues();
    private FuelPerformanceService service;

    @BeforeEach
    void setUp() {
        FuelPerformanceContextPort contexts = new FuelPerformanceContextPort() {
            public Map<UUID, VehicleContext> vehicles(Set<UUID> ids) {
                return ids.contains(VEHICLE) ? Map.of(VEHICLE, new VehicleContext(VEHICLE, "WP-TEST", TYPE, true))
                        : Map.of();
            }
            public Map<UUID, DriverContext> drivers(Set<UUID> ids) {
                return ids.contains(DRIVER) ? Map.of(DRIVER, new DriverContext(DRIVER, "Test Driver", true))
                        : Map.of();
            }
            public Map<UUID, TripContext> trips(Set<UUID> ids) {
                return ids.contains(TRIP) ? Map.of(TRIP, new TripContext(TRIP, VEHICLE, DRIVER)) : Map.of();
            }
        };
        FuelPerformanceTenantPort tenants = () -> new FuelPerformanceTenantPort.TenantContext(
                UUID.randomUUID(), "UTC", "LKR");
        service = new FuelPerformanceService(repository, contexts, tenants,
                Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void calculatesDistanceMetricsAndTwentyPercentBoundaryWithBigDecimalRounding() {
        baseline("150", "10");
        current("150", "12");

        var result = service.vehicle(VEHICLE, criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));

        assertThat(result.metrics().litresPerKm()).isEqualByComparingTo("0.120");
        assertThat(result.metrics().litresPer100Km()).isEqualByComparingTo("12.000");
        assertThat(result.metrics().kmPerLitre()).isEqualByComparingTo("8.333");
        assertThat(result.metrics().adverseVariancePercent()).isEqualByComparingTo("20.00");
        assertThat(result.metrics().indicators()).containsExactly(
                FuelPerformanceQuery.Indicator.EFFICIENCY_DEVIATION,
                FuelPerformanceQuery.Indicator.REVIEW_REQUIRED);
        assertThat(result.metrics().quality()).isEqualTo(FuelPerformanceQuery.DataQuality.COMPLETE);
    }

    @Test
    void nineteenPointNineNineDoesNotFlagDeviation() {
        baseline("150", "10");
        current("150", "11.999");
        var result = service.vehicle(VEHICLE, criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        assertThat(result.metrics().adverseVariancePercent()).isEqualByComparingTo("19.99");
        assertThat(result.metrics().indicators()).isEmpty();
    }

    @Test
    void calculatesEngineHourModeWithoutDistanceMetrics() {
        baselineHours("15", "5");
        currentHours("15", "6");
        var result = service.vehicle(VEHICLE, criteria(FuelPerformanceQuery.MeasurementMode.ENGINE_HOURS));
        assertThat(result.metrics().litresPerEngineHour()).isEqualByComparingTo("0.600");
        assertThat(result.metrics().distanceKm()).isNull();
        assertThat(result.metrics().litresPer100Km()).isNull();
    }

    @Test
    void missingAndZeroDenominatorsAreInsufficientAndNeverZero() {
        baseline("150", "10");
        repository.values.add(issue("2026-08-20T00:00:00Z", "10", null, null));
        repository.values.add(issue("2026-09-01T00:00:00Z", "10", "500", null));
        var result = service.vehicle(VEHICLE, criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        assertThat(result.metrics().quality()).isEqualTo(FuelPerformanceQuery.DataQuality.INSUFFICIENT);
        assertThat(result.metrics().consumptionRate()).isNull();
        assertThat(result.metrics().exclusionReasons()).containsEntry("MISSING_DENOMINATOR", 1);
    }

    @Test
    void negativeMeterSequenceIsInvalidSourceData() {
        baseline("150", "10");
        repository.values.add(issue("2026-08-20T00:00:00Z", "10", "500", null));
        repository.values.add(issue("2026-09-01T00:00:00Z", "10", "400", null));
        var result = service.vehicle(VEHICLE, criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        assertThat(result.metrics().quality()).isEqualTo(FuelPerformanceQuery.DataQuality.INVALID_SOURCE_DATA);
        assertThat(result.metrics().consumptionRate()).isNull();
    }

    @Test
    void twoConsecutiveThirtyPercentBucketsProducePossibleLeakageIndicator() {
        repository.values.add(issue("2026-08-23T00:00:00Z", "10", "0", null));
        repository.values.add(issue("2026-08-25T00:00:00Z", "10", "150", null));
        repository.values.add(issue("2026-08-27T00:00:00Z", "10", "300", null));
        repository.values.add(issue("2026-09-03T01:00:00Z", "13", "300", null));
        repository.values.add(issue("2026-09-03T20:00:00Z", "13", "400", null));
        repository.values.add(issue("2026-09-04T01:00:00Z", "13", "500", null));
        repository.values.add(issue("2026-09-04T20:00:00Z", "13", "600", null));
        var criteria = new FuelPerformanceQuery.Criteria(7, null, null, VEHICLE, null, null, "DIESEL",
                FuelPerformanceQuery.MeasurementMode.DISTANCE);
        var trends = service.trends(criteria);
        assertThat(trends).hasSize(2);
        assertThat(trends.get(1).indicators()).contains(
                FuelPerformanceQuery.Indicator.POSSIBLE_LEAKAGE_INDICATOR);
    }

    @Test
    void driverAttributionRequiresMatchingTripAndReturnsOnlyOperationalLabel() {
        baseline("150", "10");
        current("150", "12");
        var result = service.driver(DRIVER, criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        assertThat(result.driverLabel()).isEqualTo("Test Driver");
        assertThat(result.metrics().sampleCount()).isEqualTo(3);
    }

    @Test
    void rejectsFutureUnboundedAndOverlongRanges() {
        assertThatThrownBy(() -> service.summary(new FuelPerformanceQuery.Criteria(null,
                java.time.LocalDate.parse("2026-09-01"), null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("range");
        assertThatThrownBy(() -> service.summary(new FuelPerformanceQuery.Criteria(null,
                java.time.LocalDate.parse("2026-09-05"), java.time.LocalDate.parse("2026-09-05"),
                null, null, null, null, null))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.summary(new FuelPerformanceQuery.Criteria(null,
                java.time.LocalDate.parse("2025-01-01"), java.time.LocalDate.parse("2026-09-04"),
                null, null, null, null, null))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void analyticsLeavesSourceFactsExactlyUnchanged() {
        baseline("150", "10");
        current("150", "12");
        var before = List.copyOf(repository.values);
        service.summary(criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        service.vehicles(criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE), 0, 20,
                "consumptionRate", "asc");
        service.drivers(criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE), 0, 20,
                "sampleCount", "desc");
        service.trends(criteria(FuelPerformanceQuery.MeasurementMode.DISTANCE));
        assertThat(repository.values).containsExactlyElementsOf(before);
    }

    private void baseline(String step, String quantity) {
        repository.values.add(issue("2026-07-10T00:00:00Z", quantity, "0", null));
        repository.values.add(issue("2026-07-20T00:00:00Z", quantity, step, null));
        repository.values.add(issue("2026-08-01T00:00:00Z", quantity,
                new BigDecimal(step).multiply(BigDecimal.TWO).toPlainString(), null));
    }

    private void current(String step, String quantity) {
        repository.values.add(issue("2026-08-10T00:00:00Z", quantity, "300", null));
        repository.values.add(issue("2026-08-20T00:00:00Z", quantity,
                new BigDecimal("300").add(new BigDecimal(step)).toPlainString(), null));
        repository.values.add(issue("2026-09-01T00:00:00Z", quantity,
                new BigDecimal("300").add(new BigDecimal(step).multiply(BigDecimal.TWO)).toPlainString(), null));
    }

    private void baselineHours(String step, String quantity) {
        repository.values.add(issue("2026-07-10T00:00:00Z", quantity, null, "0"));
        repository.values.add(issue("2026-07-20T00:00:00Z", quantity, null, step));
        repository.values.add(issue("2026-08-01T00:00:00Z", quantity, null,
                new BigDecimal(step).multiply(BigDecimal.TWO).toPlainString()));
    }

    private void currentHours(String step, String quantity) {
        repository.values.add(issue("2026-08-10T00:00:00Z", quantity, null, "30"));
        repository.values.add(issue("2026-08-20T00:00:00Z", quantity, null,
                new BigDecimal("30").add(new BigDecimal(step)).toPlainString()));
        repository.values.add(issue("2026-09-01T00:00:00Z", quantity, null,
                new BigDecimal("30").add(new BigDecimal(step).multiply(BigDecimal.TWO)).toPlainString()));
    }

    private FuelPerformanceQuery.Criteria criteria(FuelPerformanceQuery.MeasurementMode mode) {
        return new FuelPerformanceQuery.Criteria(30, null, null, VEHICLE, null, null, "DIESEL", mode);
    }

    private FuelIssue issue(String date, String quantity, String odometer, String engineHours) {
        var value = new BigDecimal(quantity);
        return new FuelIssue(UUID.randomUUID(), "FI-" + repository.values.size(), VEHICLE, TRIP, DRIVER,
                "DIESEL", value, new BigDecimal("100"), value.multiply(new BigDecimal("100")), UUID.randomUUID(),
                decimal(odometer), decimal(engineHours), OffsetDateTime.parse(date), FuelIssueStatus.ISSUED,
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse(date), null,
                OffsetDateTime.parse(date), OffsetDateTime.parse(date));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static final class InMemoryIssues implements FuelIssueRepository {
        private final List<FuelIssue> values = new ArrayList<>();
        public List<FuelIssue> findIssuedBetween(OffsetDateTime from, OffsetDateTime to) { return List.copyOf(values); }
        public FuelIssue save(FuelIssue issue) { throw new UnsupportedOperationException(); }
        public Optional<FuelIssue> findById(UUID id) { return Optional.empty(); }
        public Optional<FuelIssue> findByIdForUpdate(UUID id) { return Optional.empty(); }
        public FuelIssueUseCase.PageResult<FuelIssue> search(FuelIssueUseCase.SearchQuery query) {
            throw new UnsupportedOperationException();
        }
        public List<FuelIssue> findByTripId(UUID id) { return List.of(); }
        public boolean existsByVoucherNumber(String voucherNumber) { return false; }
    }
}
