package com.transportlogistics.app.freight;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Public, read-only reporting contract owned by the Freight module. */
public interface FreightReportingQuery {
    FreightSummary summary(FreightReportCriteria criteria);

    Page<FreightShipmentReportItem> shipments(FreightReportCriteria criteria, Pageable pageable);

    List<FreightShipmentReportItem> exportShipments(FreightReportCriteria criteria, int limit);

    record FreightReportCriteria(OffsetDateTime from, OffsetDateTime to, UUID customerId, UUID freightOrderId,
                                 UUID originLocationId, UUID destinationLocationId, String loadPlanStatus,
                                 String exceptionStatus, String exceptionType, String policyStatus,
                                 String claimStatus) {}

    record FreightSummary(long freightOrders, long manifests, long manifestItems, long loadPlans,
                          Map<String, Long> loadPlansByStatus, Map<String, Long> complianceOutcomes,
                          long policies, Map<String, Long> policiesByStatus,
                          long claims, Map<String, Long> claimsByStatus, long settlements,
                          long cargoExceptions, Map<String, Long> exceptionsByStatus,
                          Map<String, Long> exceptionsByType, long unresolvedExceptions) {}

    record FreightShipmentReportItem(UUID freightOrderId, String orderNumber, UUID customerId,
                                     UUID originLocationId, UUID destinationLocationId,
                                     OffsetDateTime createdAt, OffsetDateTime requestedPickupAt,
                                     OffsetDateTime requestedDeliveryAt, UUID manifestId, String manifestNumber,
                                     boolean manifestFinalized, long manifestItemCount, UUID loadPlanId,
                                     String loadPlanNumber, String loadPlanStatus, UUID vehicleId,
                                     BigDecimal cargoWeightKg, BigDecimal cargoVolumeM3,
                                     BigDecimal payloadUtilizationPercent, BigDecimal volumeUtilizationPercent,
                                     String complianceOutcome, List<String> incompleteDiagnostics) {}
}
