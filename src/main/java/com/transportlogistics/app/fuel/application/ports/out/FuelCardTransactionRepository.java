package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardImportUseCase;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FuelCardTransactionRepository {
    Optional<FuelCardImportUseCase.Batch> findBatch(UUID tenantId, UUID providerId, String providerBatchId);
    Optional<FuelCardImportUseCase.Batch> findBatchByHash(UUID tenantId, UUID providerId, String hash);
    FuelCardImportUseCase.Batch saveBatch(FuelCardImportUseCase.Batch batch, UUID tenantId);
    List<FuelCardImportUseCase.Batch> batches(UUID tenantId, int page, int limit);
    Optional<FuelCardImportUseCase.Batch> batch(UUID tenantId, UUID id);
    Optional<FuelCardImportUseCase.Transaction> findProviderTransaction(UUID tenantId, UUID providerId, String providerTransactionId);
    boolean providerTransactionHashMatches(UUID tenantId, UUID providerId, String providerTransactionId, String hash);
    void markReversed(UUID tenantId, UUID providerId, String providerTransactionId);
    FuelCardImportUseCase.Transaction saveTransaction(FuelCardImportParser.ParsedTransaction fact, UUID tenantId,
                                                       UUID batchId, UUID providerId, UUID cardId, UUID importedBy,
                                                       String localStatus, Set<String> indicators, OffsetDateTime now);
    List<FuelCardImportUseCase.Transaction> transactions(UUID tenantId, FuelCardImportUseCase.TransactionSearch search);
    Optional<FuelCardImportUseCase.Transaction> transaction(UUID tenantId, UUID id);
    FuelCardImportUseCase.Transaction reconcile(UUID tenantId, UUID id, UUID actorId,
                                                FuelCardImportUseCase.Action action, OffsetDateTime now);
    Totals totals(UUID tenantId, UUID cardId, OffsetDateTime from, OffsetDateTime to);
    record Totals(BigDecimal amount, BigDecimal litres) {}
}
