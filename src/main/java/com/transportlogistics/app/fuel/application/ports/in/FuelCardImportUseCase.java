package com.transportlogistics.app.fuel.application.ports.in;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FuelCardImportUseCase {
    Batch importJson(Context context, UUID providerId, byte[] json);
    List<Batch> batches(UUID tenantId, int page, int limit);
    Batch batch(UUID tenantId, UUID batchId);
    List<Transaction> transactions(UUID tenantId, TransactionSearch search);
    Transaction transaction(UUID tenantId, UUID transactionId);
    Transaction reconcile(Context context, UUID transactionId, Action command);

    record Context(UUID tenantId, UUID actorId) {}
    record Action(UUID purchaseId, long version, String reason, String action) {}
    record TransactionSearch(int page, int limit, UUID cardId, UUID providerId, OffsetDateTime from,
                             OffsetDateTime to, String localStatus, String reconciliationStatus,
                             String indicator, Boolean reviewRequired, String sort, String direction) {}
    record Batch(UUID id, UUID providerId, String providerBatchId, String fileHash, OffsetDateTime generatedAt,
                 int transactionCount, int importedCount, int reviewCount, UUID importedBy, OffsetDateTime createdAt) {}
    record Transaction(UUID id, UUID batchId, UUID providerId, UUID cardId, String providerTransactionId,
                       String transactionKind, String originalProviderTransactionId, OffsetDateTime transactionTimestamp,
                       OffsetDateTime postedTimestamp, String stationReference, String fuelType,
                       BigDecimal quantityLitres, BigDecimal unitPrice, BigDecimal totalAmount, String currency,
                       UUID tripId, String providerStatus, String localStatus, UUID reconciledPurchaseId,
                       Set<String> indicators, UUID importedBy, long version, OffsetDateTime createdAt) {}
}
