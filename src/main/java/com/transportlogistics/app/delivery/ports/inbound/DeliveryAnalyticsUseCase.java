package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;

import java.util.List;

public interface DeliveryAnalyticsUseCase {
    DeliveryAnalyticsSummary getSummary(DeliveryAnalyticsCriteria criteria);
    List<FailureReasonBreakdownItem> getFailureBreakdown(DeliveryAnalyticsCriteria criteria);
    List<RegionalPerformanceItem> getRegionalPerformance(DeliveryAnalyticsCriteria criteria);
    List<DeliveryTrendItem> getTrends(DeliveryAnalyticsCriteria criteria, TrendGranularity granularity);
}
