package com.transportlogistics.app.billing.ports.inbound;

import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import com.transportlogistics.app.billing.ports.outbound.BillingStore;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransportBillingUseCase {
    TransportBillingRecord create(Context context, Create command, String key);
    List<TransportBillingRecord> list(UUID tenantId, Filters filters, int page, int size);
    TransportBillingRecord get(UUID tenantId, UUID id);
    TransportBillingRecord replace(Context context, UUID id, long version, Replacement command);
    TransportBillingRecord validate(Context context, UUID id, long version);
    TransportBillingRecord approve(Context context, UUID id, long version, String key);
    TransportBillingRecord cancel(Context context, UUID id, long version, String reason, String key);
    TransportBillingRecord finalizeRecord(Context context, UUID id, long version, String key);
    TransportBillingRecord reverse(Context context, UUID id, long version, String reason, String key);
    TransportBillingRecord export(Context context, UUID id, long version, String key);
    List<BillingStore.History> history(UUID tenantId, UUID id);
    record Context(UUID tenantId, UUID actorId, String correlationId) {}
    record Create(TransportBillingRecord.Source.SourceType sourceType, UUID sourceId, String currency,
                  UUID replacementOfRecordId) {}
    record Line(UUID id, TransportBillingRecord.LineCategory category, String reasonCode, String provenance,
                BigDecimal quantity, BigDecimal unitRate, BigDecimal amount) {}
    record Tax(TransportBillingRecord.TaxStatus status, String category, String jurisdictionReference,
               BigDecimal taxableAmount, BigDecimal rate, BigDecimal taxAmount, String exemptionReference,
               String provenance, String snapshotHash) {}
    record CostCentre(UUID id, String code, BigDecimal allocationPercent, String description, String source) {}
    record Replacement(List<Line> lines, Tax tax, List<CostCentre> costCentres) {}
    record Filters(UUID customerId, TransportBillingRecord.Source.SourceType sourceType, UUID sourceId,
                   TransportBillingRecord.Lifecycle lifecycle, TransportBillingRecord.RecordType recordType,
                   String currency, String costCentreCode, java.time.OffsetDateTime createdFrom,
                   java.time.OffsetDateTime createdTo, java.time.OffsetDateTime finalizedFrom,
                   java.time.OffsetDateTime finalizedTo, Sort sort) {}
    enum Sort { CREATED, UPDATED, FINALIZED, CUSTOMER, TOTAL, LIFECYCLE }
}
