package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliverySelfServiceAccess;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliverySelfServiceAccessRepository {
    Optional<DeliverySelfServiceAccess> findBootstrapByTokenHash(String tokenHash);
    Optional<DeliverySelfServiceAccess> findByTokenHash(String tokenHash);
    Optional<DeliverySelfServiceAccess> findByTokenHashForUpdate(String tokenHash);
    Optional<DeliverySelfServiceAccess> findByIssuanceKeyForUpdate(String key);
    List<DeliverySelfServiceAccess> findActiveForUpdate(UUID deliveryId, UUID customerId, OffsetDateTime now);
    DeliverySelfServiceAccess save(DeliverySelfServiceAccess access);
    void revoke(UUID id, OffsetDateTime at, String reason);
    boolean markUsed(UUID id, OffsetDateTime at);
}
