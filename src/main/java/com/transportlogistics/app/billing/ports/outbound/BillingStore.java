package com.transportlogistics.app.billing.ports.outbound;

import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingStore {
    Optional<TransportBillingRecord> find(UUID tenantId, UUID id);
    Optional<CreateReplay> findByCreateKey(UUID tenantId, String key);
    void lockIdempotency(UUID tenantId, String scope, String key);
    Optional<TransportBillingRecord> findByExportEvent(UUID tenantId, UUID eventId);
    List<TransportBillingRecord> list(UUID tenantId, Filter filter, int page, int size);
    String nextNumber(UUID tenantId, int year);
    TransportBillingRecord insert(TransportBillingRecord record, String key, String requestHash);
    TransportBillingRecord update(TransportBillingRecord record, long expectedVersion);
    boolean sourceClaimed(UUID tenantId, String sourceType, UUID sourceId, UUID excludingRecord);
    boolean reversalExists(UUID tenantId, UUID originalId, UUID excludingRecord);
    Optional<CommandReplay> command(UUID tenantId, String scope, String key);
    void command(UUID tenantId, UUID recordId, String scope, String key, String requestHash,
                 UUID actorId, long resultVersion, OffsetDateTime at);
    void history(UUID tenantId, UUID recordId, String action, String fromState, String toState,
                 UUID actorId, String detail, OffsetDateTime at);
    List<History> history(UUID tenantId, UUID recordId);
    record CommandReplay(String requestHash, UUID recordId, long resultVersion) {}
    record CreateReplay(String requestHash, TransportBillingRecord record) {}
    record Filter(UUID customerId, String sourceType, UUID sourceId, String lifecycle, String recordType,
                  String currency, String costCentreCode, OffsetDateTime createdFrom, OffsetDateTime createdTo,
                  OffsetDateTime finalizedFrom, OffsetDateTime finalizedTo, String sort) {}
    record History(UUID id, String action, String fromState, String toState, UUID actorId,
                   String detail, OffsetDateTime createdAt) {}
}
