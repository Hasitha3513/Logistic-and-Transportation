package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAnalyticsPersistencePort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAnalyticsPersistencePort.RawRegionalPerformance;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort.LocationReference;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort.TenantContext;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAnalyticsServiceTest {

    @Mock
    private DeliveryAnalyticsPersistencePort persistencePort;

    @Mock
    private DeliveryTenantContextPort tenantContextPort;

    @Mock
    private DeliveryLocationLookupPort locationLookupPort;

    private DeliveryAnalyticsService service;

    private final UUID tenantId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private final String timeZone = "Asia/Colombo";

    @BeforeEach
    void setUp() {
        service = new DeliveryAnalyticsService(persistencePort, tenantContextPort, locationLookupPort);
        when(tenantContextPort.currentTenant()).thenReturn(Optional.of(new TenantContext(tenantId, timeZone)));
    }

    @Test
    @DisplayName("Should return delivery analytics summary with correct mathematical calculations")
    void shouldReturnSummary() {
        var summaryStub = new DeliveryAnalyticsSummary(
                null,
                100L,
                10L,
                90L,
                80L,
                10L,
                BigDecimal.valueOf(88.89),
                BigDecimal.valueOf(75.00),
                70L,
                10L,
                BigDecimal.valueOf(87.50),
                BigDecimal.valueOf(12.50),
                BigDecimal.valueOf(35.5),
                25L,
                BigDecimal.valueOf(0.25),
                15L,
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(80.00),
                BigDecimal.valueOf(11.11)
        );

        when(persistencePort.querySummary(eq(tenantId), any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(summaryStub);

        var criteria = new DeliveryAnalyticsCriteria(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null,
                null,
                null
        );

        var result = service.getSummary(criteria);

        assertThat(result).isNotNull();
        assertThat(result.totalOrders()).isEqualTo(100L);
        assertThat(result.orderSuccessRate()).isEqualTo(BigDecimal.valueOf(88.89));
        assertThat(result.firstAttemptSuccessRate()).isEqualTo(BigDecimal.valueOf(75.00));
        assertThat(result.onTimeDeliveryRate()).isEqualTo(BigDecimal.valueOf(87.50));
        assertThat(result.averageDelayMinutes()).isEqualTo(BigDecimal.valueOf(35.5));
        assertThat(result.redeliverySuccessRate()).isEqualTo(BigDecimal.valueOf(80.00));
        assertThat(result.returnToBaseRate()).isEqualTo(BigDecimal.valueOf(11.11));
    }

    @Test
    @DisplayName("Should return regional performance with resolved location names and fallback to UNCLASSIFIED")
    void shouldReturnRegionalPerformance() {
        UUID locId = UUID.randomUUID();
        UUID unclassifiedLocId = UUID.randomUUID();

        when(locationLookupPort.findLocation(locId))
                .thenReturn(Optional.of(new LocationReference(locId, "LOC-CMB", "Colombo Central Hub", true)));
        when(locationLookupPort.findLocation(unclassifiedLocId))
                .thenReturn(Optional.empty());

        var rawList = List.of(
                new RawRegionalPerformance(locId, 50, 45, 5, 40, 200, 5, 10),
                new RawRegionalPerformance(unclassifiedLocId, 10, 8, 2, 8, 0, 0, 1)
        );

        when(persistencePort.queryRegionalPerformance(eq(tenantId), any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(rawList);

        var result = service.getRegionalPerformance(null);

        assertThat(result).hasSize(2);

        var item1 = result.get(0);
        assertThat(item1.locationCode()).isEqualTo("LOC-CMB");
        assertThat(item1.locationName()).isEqualTo("Colombo Central Hub");
        assertThat(item1.orderSuccessRate()).isEqualTo(BigDecimal.valueOf(90.00).setScale(2));
        assertThat(item1.onTimeDeliveryRate()).isEqualTo(BigDecimal.valueOf(88.89));
        assertThat(item1.averageDelayMinutes()).isEqualTo(BigDecimal.valueOf(40.0).setScale(1));

        var item2 = result.get(1);
        assertThat(item2.locationCode()).isEqualTo("UNCLASSIFIED");
        assertThat(item2.locationName()).isEqualTo("Unclassified Location");
        assertThat(item2.averageDelayMinutes()).isNull(); // 0 late deliveries -> null
    }

    @Test
    @DisplayName("Should return failure reason breakdown")
    void shouldReturnFailureBreakdown() {
        var items = List.of(
                new FailureReasonBreakdownItem("CUSTOMER_UNAVAILABLE", 10L, BigDecimal.valueOf(50.00), 10L, 0L, 0L),
                new FailureReasonBreakdownItem("CUSTOMER_REFUSED", 10L, BigDecimal.valueOf(50.00), 0L, 10L, 0L)
        );

        when(persistencePort.queryFailureBreakdown(eq(tenantId), any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(items);

        var result = service.getFailureBreakdown(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).failureReason()).isEqualTo("CUSTOMER_UNAVAILABLE");
        assertThat(result.get(0).redeliveryEligibleCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should validate date range boundaries and reject inverted or excessive ranges")
    void shouldValidateDateRange() {
        var invertedCriteria = new DeliveryAnalyticsCriteria(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 1),
                null, null, null
        );

        assertThatThrownBy(() -> service.getSummary(invertedCriteria))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be after");

        var excessiveCriteria = new DeliveryAnalyticsCriteria(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 8, 1),
                null, null, null
        );

        assertThatThrownBy(() -> service.getSummary(excessiveCriteria))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot exceed 365 days");
    }

    @Test
    @DisplayName("Should validate filter values and reject invalid serviceType or priority")
    void shouldValidateFilters() {
        var invalidService = new DeliveryAnalyticsCriteria(null, null, "DRONE_AIRDROP", null, null);
        assertThatThrownBy(() -> service.getSummary(invalidService))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid service type");

        var invalidPriority = new DeliveryAnalyticsCriteria(null, null, null, "SUPER_HIGH", null);
        assertThatThrownBy(() -> service.getSummary(invalidPriority))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid priority");

        UUID missingLoc = UUID.randomUUID();
        when(locationLookupPort.findLocation(missingLoc)).thenReturn(Optional.empty());
        var missingLocCriteria = new DeliveryAnalyticsCriteria(null, null, null, null, missingLoc);

        assertThatThrownBy(() -> service.getSummary(missingLocCriteria))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Destination location not found");
    }
}
