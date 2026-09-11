package com.transportlogistics.app.billing.adapters.inbound.web.dto.response;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
public record BillingResponse(UUID id,String billingNumber,String recordType,UUID originalBillingRecordId,
 UUID replacementOfRecordId,Source source,UUID customerId,String currency,String lifecycle,List<Line> lines,
 Tax tax,List<CostCentre> costCentres,Totals totals,UUID preparedBy,UUID approvedBy,OffsetDateTime approvedAt,
 OffsetDateTime finalizedAt,UUID exportEventId,String compliance,long version,OffsetDateTime createdAt,OffsetDateTime updatedAt){
 public record Source(String type,UUID id,String businessNumber,String terminalLifecycle,OffsetDateTime completionTime,long sourceVersion,String snapshotHash){}
 public record Line(UUID id,String category,String reasonCode,String provenance,BigDecimal quantity,BigDecimal unitRate,BigDecimal amount){}
 public record Tax(String status,String category,String jurisdictionReference,BigDecimal taxableAmount,BigDecimal rate,BigDecimal taxAmount,String exemptionReference,String provenance,String snapshotHash){}
 public record CostCentre(UUID id,String code,BigDecimal allocationPercent,String description,String source){}
 public record Totals(BigDecimal baseCharge,BigDecimal surcharges,BigDecimal penalties,BigDecimal creditAdjustments,BigDecimal subtotal,BigDecimal taxAmount,BigDecimal totalAmount){}
 public record History(UUID id,String action,String fromState,String toState,UUID actorId,String detail,OffsetDateTime createdAt){}
}
