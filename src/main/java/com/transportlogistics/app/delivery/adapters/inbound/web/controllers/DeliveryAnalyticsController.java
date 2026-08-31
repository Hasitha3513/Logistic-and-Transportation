package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryAnalyticsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/analytics")
@Validated
public class DeliveryAnalyticsController {

    private final DeliveryAnalyticsUseCase analyticsUseCase;

    public DeliveryAnalyticsController(DeliveryAnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('DELIVERY_ANALYTICS_VIEW')")
    public ResponseEntity<DeliveryAnalyticsSummary> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID destinationLocationId
    ) {
        var criteria = new DeliveryAnalyticsCriteria(from, to, serviceType, priority, destinationLocationId);
        return ResponseEntity.ok(analyticsUseCase.getSummary(criteria));
    }

    @GetMapping("/failures")
    @PreAuthorize("hasAuthority('DELIVERY_ANALYTICS_VIEW')")
    public ResponseEntity<List<FailureReasonBreakdownItem>> getFailures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID destinationLocationId
    ) {
        var criteria = new DeliveryAnalyticsCriteria(from, to, serviceType, priority, destinationLocationId);
        return ResponseEntity.ok(analyticsUseCase.getFailureBreakdown(criteria));
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAuthority('DELIVERY_ANALYTICS_VIEW')")
    public ResponseEntity<List<RegionalPerformanceItem>> getRegions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID destinationLocationId
    ) {
        var criteria = new DeliveryAnalyticsCriteria(from, to, serviceType, priority, destinationLocationId);
        return ResponseEntity.ok(analyticsUseCase.getRegionalPerformance(criteria));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAuthority('DELIVERY_ANALYTICS_VIEW')")
    public ResponseEntity<List<DeliveryTrendItem>> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID destinationLocationId,
            @RequestParam(defaultValue = "DAY") String granularity
    ) {
        var criteria = new DeliveryAnalyticsCriteria(from, to, serviceType, priority, destinationLocationId);
        TrendGranularity gran;
        try {
            gran = TrendGranularity.valueOf(granularity.toUpperCase());
        } catch (IllegalArgumentException ex) {
            gran = TrendGranularity.DAY;
        }
        return ResponseEntity.ok(analyticsUseCase.getTrends(criteria, gran));
    }
}
