package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.freight.FreightReportingQuery.FreightShipmentReportItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FreightReportResponse {
    private FreightReportResponse() {}

    public record Summary(long freightOrders, long manifests, long manifestItems, long loadPlans,
                          Map<String, Long> loadPlansByStatus, Map<String, Long> complianceOutcomes,
                          long policies, Map<String, Long> policiesByStatus, long claims,
                          Map<String, Long> claimsByStatus, long settlements, long cargoExceptions,
                          Map<String, Long> exceptionsByStatus, Map<String, Long> exceptionsByType,
                          long unresolvedExceptions) {}

    public static Summary summary(com.transportlogistics.app.freight.FreightReportingQuery.FreightSummary value) {
        return new Summary(value.freightOrders(), value.manifests(), value.manifestItems(), value.loadPlans(),
                value.loadPlansByStatus(), value.complianceOutcomes(), value.policies(), value.policiesByStatus(),
                value.claims(), value.claimsByStatus(), value.settlements(), value.cargoExceptions(),
                value.exceptionsByStatus(), value.exceptionsByType(), value.unresolvedExceptions());
    }

    public record Shipment(UUID freightOrderId, String orderNumber, UUID customerId, UUID originLocationId,
                           UUID destinationLocationId, OffsetDateTime createdAt, OffsetDateTime requestedPickupAt,
                           OffsetDateTime requestedDeliveryAt,
                           UUID manifestId, String manifestNumber, boolean manifestFinalized, long manifestItemCount,
                           UUID loadPlanId, String loadPlanNumber, String loadPlanStatus, UUID vehicleId,
                           BigDecimal cargoWeightKg, BigDecimal cargoVolumeM3, BigDecimal payloadUtilizationPercent,
                           BigDecimal volumeUtilizationPercent, String complianceOutcome,
                           List<String> incompleteDiagnostics) {}

    public static Shipment shipment(FreightShipmentReportItem value) {
        return new Shipment(value.freightOrderId(), value.orderNumber(), value.customerId(), value.originLocationId(),
                value.destinationLocationId(), value.createdAt(), value.requestedPickupAt(), value.requestedDeliveryAt(),
                value.manifestId(), value.manifestNumber(), value.manifestFinalized(), value.manifestItemCount(),
                value.loadPlanId(), value.loadPlanNumber(), value.loadPlanStatus(), value.vehicleId(),
                value.cargoWeightKg(), value.cargoVolumeM3(), value.payloadUtilizationPercent(),
                value.volumeUtilizationPercent(), value.complianceOutcome(), value.incompleteDiagnostics());
    }
}
