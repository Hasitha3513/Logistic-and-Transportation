package com.transportlogistics.app.delivery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public, read-only delivery analytics and reporting contract owned by the Delivery module.
 */
public interface DeliveryReportingQuery {

    DeliveryAnalyticsSummary getSummary(DeliveryAnalyticsCriteria criteria);

    List<FailureReasonBreakdownItem> getFailureBreakdown(DeliveryAnalyticsCriteria criteria);

    List<RegionalPerformanceItem> getRegionalPerformance(DeliveryAnalyticsCriteria criteria);

    List<DeliveryTrendItem> getTrends(DeliveryAnalyticsCriteria criteria, TrendGranularity granularity);

    enum TrendGranularity {
        DAY,
        WEEK,
        MONTH
    }

    record DeliveryAnalyticsCriteria(
            LocalDate from,
            LocalDate to,
            String serviceType,
            String priority,
            UUID destinationLocationId
    ) {}

    record Period(
            LocalDate from,
            LocalDate to
    ) {}

    record DeliveryAnalyticsSummary(
            Period period,
            long totalOrders,
            long activeOrders,
            long terminalCompletedOrders,
            long deliveredOrders,
            long returnedToBaseOrders,
            BigDecimal orderSuccessRate,
            BigDecimal firstAttemptSuccessRate,
            long onTimeDeliveredOrders,
            long lateDeliveredOrders,
            BigDecimal onTimeDeliveryRate,
            BigDecimal lateDeliveryRate,
            BigDecimal averageDelayMinutes,
            long totalFailedAttempts,
            BigDecimal averageFailedAttemptsPerOrder,
            long redeliveredOrders,
            BigDecimal redeliveryRate,
            BigDecimal redeliverySuccessRate,
            BigDecimal returnToBaseRate
    ) {}

    record FailureReasonBreakdownItem(
            String failureReason,
            long count,
            BigDecimal percentage,
            long redeliveryEligibleCount,
            long returnToBaseCount,
            long escalatedCount
    ) {}

    record RegionalPerformanceItem(
            UUID destinationLocationId,
            String locationCode,
            String locationName,
            String city,
            long totalOrders,
            long deliveredOrders,
            long returnedToBaseOrders,
            BigDecimal orderSuccessRate,
            long onTimeDeliveredOrders,
            BigDecimal onTimeDeliveryRate,
            BigDecimal averageDelayMinutes,
            long failedAttemptCount
    ) {}

    record DeliveryTrendItem(
            LocalDate bucketDate,
            long totalCreated,
            long delivered,
            long failedAttempts,
            long returnedToBase,
            long onTimeDelivered,
            long lateDelivered
    ) {}
}
