package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface DeliveryAnalyticsPersistencePort {

    DeliveryAnalyticsSummary querySummary(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                         String serviceType, String priority, UUID destinationLocationId);

    List<FailureReasonBreakdownItem> queryFailureBreakdown(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                                          String serviceType, String priority, UUID destinationLocationId);

    List<RawRegionalPerformance> queryRegionalPerformance(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                                          String serviceType, String priority, UUID destinationLocationId);

    List<DeliveryTrendItem> queryTrends(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                        String serviceType, String priority, UUID destinationLocationId,
                                        TrendGranularity granularity, ZoneId zoneId);

    record RawRegionalPerformance(
            UUID destinationLocationId,
            long totalOrders,
            long deliveredOrders,
            long returnedToBaseOrders,
            long onTimeDeliveredOrders,
            long totalDelayMinutes,
            long lateDeliveredOrders,
            long failedAttemptCount
    ) {}
}
