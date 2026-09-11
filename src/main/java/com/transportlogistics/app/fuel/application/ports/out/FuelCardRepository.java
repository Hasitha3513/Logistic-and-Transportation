package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelCardRepository {
    FuelCard save(FuelCard card);
    Optional<FuelCard> find(UUID tenantId, UUID id);
    List<FuelCard> list(UUID tenantId, FuelCardUseCase.Search search);
    boolean referenceExists(UUID tenantId, UUID providerId, String reference);
    Optional<FuelCard> findByProviderReference(UUID tenantId, UUID providerId, String reference);
    boolean hasActiveBinding(UUID tenantId, UUID cardId);
    boolean hasRestriction(UUID tenantId, UUID cardId);
    Optional<FuelCardUseCase.Restriction> restriction(UUID tenantId, UUID cardId);
    Optional<FuelCardUseCase.Binding> activeBinding(UUID tenantId, UUID cardId);
    FuelCardUseCase.Binding replaceBinding(UUID tenantId, UUID cardId, FuelCardUseCase.Bind command,
                                           UUID actorId, java.time.OffsetDateTime now);
    List<FuelCardUseCase.Binding> bindings(UUID tenantId, UUID cardId);
    FuelCardUseCase.Restriction replaceRestriction(UUID tenantId, UUID cardId,
                                                   FuelCardUseCase.Restrict command, UUID actorId,
                                                   java.time.OffsetDateTime now);
    void audit(UUID tenantId, UUID cardId, UUID transactionId, String action, String result,
               String reasonCode, UUID actorId, java.time.OffsetDateTime now);
    List<FuelCardUseCase.History> history(UUID tenantId, UUID cardId, int page, int limit);
}
