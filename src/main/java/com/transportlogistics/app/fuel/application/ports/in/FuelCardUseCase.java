package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelCard;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FuelCardUseCase {
    FuelCard create(Context context, Create command);
    FuelCard get(UUID tenantId, UUID cardId);
    FuelCard update(Context context, UUID cardId, Update command);
    List<FuelCard> list(UUID tenantId, Search search);
    FuelCard transition(Context context, UUID cardId, FuelCard.Status target, long version, String reason);
    Binding bind(Context context, UUID cardId, Bind command);
    List<Binding> bindings(UUID tenantId, UUID cardId);
    Restriction restrict(Context context, UUID cardId, Restrict command);
    List<History> history(UUID tenantId, UUID cardId, int page, int limit);

    record Context(UUID tenantId, UUID actorId) {}
    record Create(UUID providerId, String alias, String providerCardReference, String maskedIdentifier,
                  String lastFour, int expiryMonth, int expiryYear) {}
    record Update(String alias, int expiryMonth, int expiryYear, long version) {}
    record Search(int page, int limit, FuelCard.Status status, UUID providerId, String bindingType,
                  UUID bindingId, Integer expiryFrom, Integer expiryTo, Boolean reviewRequired,
                  String sort, String direction) {}
    record Bind(String bindingType, UUID bindingId, long version, String reason) {}
    record Restrict(String currency, BigDecimal maxTransactionAmount, BigDecimal maxDailyAmount,
                    BigDecimal maxMonthlyAmount, BigDecimal maxDailyLitres, Set<String> allowedFuelTypes,
                    Set<String> allowedStationReferences, long version, String reason) {}
    record Binding(UUID id, String bindingType, UUID bindingId, java.time.OffsetDateTime effectiveFrom,
                   java.time.OffsetDateTime effectiveTo, String reason) {}
    record Restriction(String currency, BigDecimal maxTransactionAmount, BigDecimal maxDailyAmount,
                       BigDecimal maxMonthlyAmount, BigDecimal maxDailyLitres, Set<String> allowedFuelTypes,
                       Set<String> allowedStationReferences, long version) {}
    record History(UUID id, String action, String result, String reasonCode, UUID actorId,
                   java.time.OffsetDateTime createdAt) {}
}
