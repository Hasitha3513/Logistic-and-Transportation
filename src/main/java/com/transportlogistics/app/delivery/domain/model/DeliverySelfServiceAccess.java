package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record DeliverySelfServiceAccess(UUID id, UUID tenantId, UUID deliveryOrderId, UUID customerId,
        String recipientContactHash, String contactHashKeyVersion, String tokenHash,
        Set<SelfServiceAction> allowedActions, String issuanceIdempotencyKey, OffsetDateTime issuedAt,
        OffsetDateTime expiresAt, OffsetDateTime revokedAt, OffsetDateTime lastUsedAt, long useCount, long version) {

    public boolean permits(SelfServiceAction action, OffsetDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt) && allowedActions.contains(action);
    }

    public DeliverySelfServiceAccess rotate(String newTokenHash, String contactHash, String keyVersion,
                                             Set<SelfServiceAction> actions, OffsetDateTime now) {
        return new DeliverySelfServiceAccess(id, tenantId, deliveryOrderId, customerId, contactHash, keyVersion,
                newTokenHash, Set.copyOf(actions), issuanceIdempotencyKey, now, now.plusDays(30), null,
                lastUsedAt, useCount, version);
    }
}
