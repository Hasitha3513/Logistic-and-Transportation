package com.transportlogistics.app.delivery.ports.inbound;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.DeliveryAnalyticsCriteria;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.DeliveryAnalyticsSummary;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.DeliveryTrendItem;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.FailureReasonBreakdownItem;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.RegionalPerformanceItem;
import com.transportlogistics.app.delivery.DeliveryReportingQuery.TrendGranularity;


import java.util.List;

public interface DeliveryAnalyticsUseCase {
    DeliveryAnalyticsSummary getSummary(DeliveryAnalyticsCriteria criteria);
    List<FailureReasonBreakdownItem> getFailureBreakdown(DeliveryAnalyticsCriteria criteria);
    List<RegionalPerformanceItem> getRegionalPerformance(DeliveryAnalyticsCriteria criteria);
    List<DeliveryTrendItem> getTrends(DeliveryAnalyticsCriteria criteria, TrendGranularity granularity);
}
