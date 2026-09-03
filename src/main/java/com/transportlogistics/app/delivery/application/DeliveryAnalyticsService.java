package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.DeliveryReportingQuery;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryAnalyticsUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAnalyticsPersistencePort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DeliveryAnalyticsService implements DeliveryAnalyticsUseCase, DeliveryReportingQuery {

    private static final int MAX_RANGE_DAYS = 365;
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final DeliveryAnalyticsPersistencePort analyticsPersistencePort;
    private final DeliveryTenantContextPort tenantContextPort;
    private final DeliveryLocationLookupPort locationLookupPort;

    public DeliveryAnalyticsService(
            DeliveryAnalyticsPersistencePort analyticsPersistencePort,
            DeliveryTenantContextPort tenantContextPort,
            DeliveryLocationLookupPort locationLookupPort
    ) {
        this.analyticsPersistencePort = analyticsPersistencePort;
        this.tenantContextPort = tenantContextPort;
        this.locationLookupPort = locationLookupPort;
    }

    @Override
    public DeliveryAnalyticsSummary getSummary(DeliveryAnalyticsCriteria criteria) {
        var context = resolveTenantContext();
        var dateRange = resolveDateRange(criteria, context.timeZone());
        validateFilters(criteria);

        var summary = analyticsPersistencePort.querySummary(
                context.tenantId(),
                dateRange.fromUtc(),
                dateRange.toUtc(),
                criteria != null ? criteria.serviceType() : null,
                criteria != null ? criteria.priority() : null,
                criteria != null ? criteria.destinationLocationId() : null
        );

        return new DeliveryAnalyticsSummary(
                new Period(dateRange.fromLocal(), dateRange.toLocal()),
                summary.totalOrders(),
                summary.activeOrders(),
                summary.terminalCompletedOrders(),
                summary.deliveredOrders(),
                summary.returnedToBaseOrders(),
                summary.orderSuccessRate(),
                summary.firstAttemptSuccessRate(),
                summary.onTimeDeliveredOrders(),
                summary.lateDeliveredOrders(),
                summary.onTimeDeliveryRate(),
                summary.lateDeliveryRate(),
                summary.averageDelayMinutes(),
                summary.totalFailedAttempts(),
                summary.averageFailedAttemptsPerOrder(),
                summary.redeliveredOrders(),
                summary.redeliveryRate(),
                summary.redeliverySuccessRate(),
                summary.returnToBaseRate()
        );
    }

    @Override
    public List<FailureReasonBreakdownItem> getFailureBreakdown(DeliveryAnalyticsCriteria criteria) {
        var context = resolveTenantContext();
        var dateRange = resolveDateRange(criteria, context.timeZone());
        validateFilters(criteria);

        return analyticsPersistencePort.queryFailureBreakdown(
                context.tenantId(),
                dateRange.fromUtc(),
                dateRange.toUtc(),
                criteria != null ? criteria.serviceType() : null,
                criteria != null ? criteria.priority() : null,
                criteria != null ? criteria.destinationLocationId() : null
        );
    }

    @Override
    public List<RegionalPerformanceItem> getRegionalPerformance(DeliveryAnalyticsCriteria criteria) {
        var context = resolveTenantContext();
        var dateRange = resolveDateRange(criteria, context.timeZone());
        validateFilters(criteria);

        var rawList = analyticsPersistencePort.queryRegionalPerformance(
                context.tenantId(),
                dateRange.fromUtc(),
                dateRange.toUtc(),
                criteria != null ? criteria.serviceType() : null,
                criteria != null ? criteria.priority() : null,
                criteria != null ? criteria.destinationLocationId() : null
        );

        List<RegionalPerformanceItem> items = new ArrayList<>();
        for (var raw : rawList) {
            String code = "UNCLASSIFIED";
            String name = "Unclassified Location";
            String city = "Unclassified";

            if (raw.destinationLocationId() != null) {
                var locOpt = locationLookupPort.findLocation(raw.destinationLocationId());
                if (locOpt.isPresent()) {
                    var loc = locOpt.get();
                    code = loc.code();
                    name = loc.name();
                    city = loc.name(); // LocationReference carries code & name
                }
            }

            long terminalOutcomes = raw.deliveredOrders() + raw.returnedToBaseOrders();
            BigDecimal orderSuccessRate = terminalOutcomes > 0
                    ? BigDecimal.valueOf(raw.deliveredOrders() * 100.0 / terminalOutcomes).setScale(2, RoundingMode.HALF_UP)
                    : null;

            BigDecimal onTimeRate = raw.deliveredOrders() > 0
                    ? BigDecimal.valueOf(raw.onTimeDeliveredOrders() * 100.0 / raw.deliveredOrders()).setScale(2, RoundingMode.HALF_UP)
                    : null;

            BigDecimal avgDelay = raw.lateDeliveredOrders() > 0
                    ? BigDecimal.valueOf(raw.totalDelayMinutes() * 1.0 / raw.lateDeliveredOrders()).setScale(1, RoundingMode.HALF_UP)
                    : null;

            items.add(new RegionalPerformanceItem(
                    raw.destinationLocationId(),
                    code,
                    name,
                    city,
                    raw.totalOrders(),
                    raw.deliveredOrders(),
                    raw.returnedToBaseOrders(),
                    orderSuccessRate,
                    raw.onTimeDeliveredOrders(),
                    onTimeRate,
                    avgDelay,
                    raw.failedAttemptCount()
            ));
        }

        return items;
    }

    @Override
    public List<DeliveryTrendItem> getTrends(DeliveryAnalyticsCriteria criteria, TrendGranularity granularity) {
        var context = resolveTenantContext();
        var dateRange = resolveDateRange(criteria, context.timeZone());
        validateFilters(criteria);

        TrendGranularity gran = granularity != null ? granularity : TrendGranularity.DAY;
        ZoneId zoneId = ZoneId.of(context.timeZone() != null ? context.timeZone() : "Asia/Colombo");

        return analyticsPersistencePort.queryTrends(
                context.tenantId(),
                dateRange.fromUtc(),
                dateRange.toUtc(),
                criteria != null ? criteria.serviceType() : null,
                criteria != null ? criteria.priority() : null,
                criteria != null ? criteria.destinationLocationId() : null,
                gran,
                zoneId
        );
    }

    private DeliveryTenantContextPort.TenantContext resolveTenantContext() {
        return tenantContextPort.currentTenant()
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Active tenant context is required for analytics"));
    }

    private void validateFilters(DeliveryAnalyticsCriteria criteria) {
        if (criteria == null) {
            return;
        }
        if (criteria.serviceType() != null && !criteria.serviceType().isBlank()
                && !List.of("STANDARD", "EXPRESS", "SAME_DAY", "SCHEDULED").contains(criteria.serviceType().toUpperCase())) {
            throw new BusinessRuleException("INVALID_SERVICE_TYPE", "Invalid service type: " + criteria.serviceType());
        }
        if (criteria.priority() != null && !criteria.priority().isBlank()
                && !List.of("LOW", "NORMAL", "HIGH", "URGENT").contains(criteria.priority().toUpperCase())) {
            throw new BusinessRuleException("INVALID_PRIORITY", "Invalid priority: " + criteria.priority());
        }
        if (criteria.destinationLocationId() != null) {
            var loc = locationLookupPort.findLocation(criteria.destinationLocationId());
            if (loc.isEmpty()) {
                throw new NotFoundException("LOCATION_NOT_FOUND", "Destination location not found in active tenant: " + criteria.destinationLocationId());
            }
        }
    }

    private DateRangeComputation resolveDateRange(DeliveryAnalyticsCriteria criteria, String timeZoneStr) {
        ZoneId zoneId = ZoneId.of(timeZoneStr != null ? timeZoneStr : "Asia/Colombo");
        LocalDate today = LocalDate.now(zoneId);

        LocalDate fromLocal = (criteria != null && criteria.from() != null) ? criteria.from() : today.minusDays(DEFAULT_RANGE_DAYS);
        LocalDate toLocal = (criteria != null && criteria.to() != null) ? criteria.to() : today;

        if (fromLocal.isAfter(toLocal)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "Query date 'from' cannot be after 'to'");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromLocal, toLocal);
        if (daysBetween > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("RANGE_EXCEEDED", "Analytics date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }

        OffsetDateTime fromUtc = fromLocal.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime toUtc = toLocal.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        return new DateRangeComputation(fromLocal, toLocal, fromUtc, toUtc);
    }

    private record DateRangeComputation(
            LocalDate fromLocal,
            LocalDate toLocal,
            OffsetDateTime fromUtc,
            OffsetDateTime toUtc
    ) {}
}
