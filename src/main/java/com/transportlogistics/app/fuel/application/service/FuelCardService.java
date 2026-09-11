package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardReferencePort;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceTenantPort;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

public final class FuelCardService implements FuelCardUseCase {
    private final FuelCardRepository cards; private final FuelCardReferencePort references;
    private final FuelPerformanceTenantPort tenants; private final Clock clock;
    public FuelCardService(FuelCardRepository cards, FuelCardReferencePort references,
                           FuelPerformanceTenantPort tenants, Clock clock) {
        this.cards = cards; this.references = references; this.tenants = tenants; this.clock = clock;
    }
    @Override public FuelCard create(Context c, Create x) {
        if (!references.providerActive(x.providerId())) throw new NotFoundException("FUEL_CARD_NOT_FOUND", "Fuel-card provider not found");
        if (cards.referenceExists(c.tenantId(), x.providerId(), x.providerCardReference()))
            throw new ConflictException("FUEL_CARD_CONFLICT", "Provider card reference already exists");
        var now = OffsetDateTime.now(clock);
        FuelCard created = cards.save(new FuelCard(UUID.randomUUID(), c.tenantId(), x.providerId(), x.alias().trim(),
                x.providerCardReference(), x.maskedIdentifier(), x.lastFour(), x.expiryMonth(), x.expiryYear(),
                FuelCard.Status.DRAFT, 0, c.actorId(), now, now));
        cards.audit(c.tenantId(), created.id(), null, "CREATE", "SUCCESS", null, c.actorId(), now);
        return created;
    }
    @Override public FuelCard get(UUID tenantId, UUID cardId) { return required(tenantId, cardId); }
    @Override public FuelCard update(Context c, UUID id, Update x) {
        FuelCard card = required(c.tenantId(), id);
        if (card.status() != FuelCard.Status.DRAFT) throw rule("FUEL_CARD_INVALID_STATE");
        if (card.version() != x.version()) throw conflict("FUEL_CARD_CONFLICT");
        var now = OffsetDateTime.now(clock);
        FuelCard updated = cards.save(new FuelCard(card.id(), card.tenantId(), card.providerId(), x.alias(),
                card.providerCardReference(), card.maskedIdentifier(), card.lastFour(), x.expiryMonth(), x.expiryYear(),
                card.status(), card.version() + 1, card.createdBy(), card.createdAt(), now));
        cards.audit(c.tenantId(), id, null, "DRAFT_UPDATED", "SUCCESS", null, c.actorId(), now);
        return updated;
    }
    @Override public List<FuelCard> list(UUID tenantId, Search search) {
        return cards.list(tenantId, new Search(Math.max(0, search.page()), Math.min(100, Math.max(1, search.limit())),
                search.status(), search.providerId(), search.bindingType(), search.bindingId(), search.expiryFrom(),
                search.expiryTo(), search.reviewRequired(), search.sort(), search.direction()));
    }
    @Override public FuelCard transition(Context c, UUID id, FuelCard.Status target, long version, String reason) {
        FuelCard card = required(c.tenantId(), id);
        if (card.version() != version) throw conflict("FUEL_CARD_CONFLICT");
        if ((target == FuelCard.Status.BLOCKED || target == FuelCard.Status.CANCELLED
                || target == FuelCard.Status.SUSPENDED) && (reason == null || reason.isBlank()))
            throw rule("FUEL_CARD_INVALID_STATE");
        if ((target == FuelCard.Status.ACTIVE) && (!cards.hasActiveBinding(c.tenantId(), id)
                || !cards.hasRestriction(c.tenantId(), id))) throw rule("FUEL_CARD_INVALID_STATE");
        var now = OffsetDateTime.now(clock);
        FuelCard changed;
        try {
            changed = cards.save(card.transition(target, now, ZoneId.of(tenants.required().timeZone())));
        } catch (IllegalStateException exception) {
            throw rule(exception.getMessage());
        }
        cards.audit(c.tenantId(), id, null, target.name(), "SUCCESS", reason, c.actorId(), now);
        return changed;
    }
    @Override public Binding bind(Context c, UUID id, Bind x) {
        FuelCard card = required(c.tenantId(), id);
        if (card.version() != x.version() || x.reason() == null || x.reason().isBlank())
            throw rule("FUEL_CARD_BINDING_INVALID");
        boolean valid = switch (x.bindingType().toUpperCase(Locale.ROOT)) {
            case "VEHICLE" -> references.vehicleActive(x.bindingId());
            case "DRIVER" -> references.driverActive(x.bindingId());
            default -> false;
        };
        if (!valid) throw rule("FUEL_CARD_BINDING_INVALID");
        Binding result = cards.replaceBinding(c.tenantId(), id, x, c.actorId(), OffsetDateTime.now(clock));
        cards.save(new FuelCard(card.id(), card.tenantId(), card.providerId(), card.alias(), card.providerCardReference(),
                card.maskedIdentifier(), card.lastFour(), card.expiryMonth(), card.expiryYear(), card.status(),
                card.version() + 1, card.createdBy(), card.createdAt(), OffsetDateTime.now(clock)));
        cards.audit(c.tenantId(), id, null, "BINDING_CHANGED", "SUCCESS", x.reason(), c.actorId(), OffsetDateTime.now(clock));
        return result;
    }
    @Override public List<Binding> bindings(UUID tenantId, UUID cardId) { required(tenantId, cardId); return cards.bindings(tenantId, cardId); }
    @Override public Restriction restrict(Context c, UUID id, Restrict x) {
        FuelCard card = required(c.tenantId(), id);
        if (card.version() != x.version() || x.reason() == null || x.reason().isBlank()
                || x.allowedFuelTypes() == null || x.allowedFuelTypes().isEmpty()
                || x.maxTransactionAmount() == null || x.maxTransactionAmount().signum() <= 0
                || x.maxDailyAmount() == null || x.maxDailyAmount().signum() <= 0
                || x.maxMonthlyAmount() == null || x.maxMonthlyAmount().signum() <= 0
                || x.maxDailyLitres() == null || x.maxDailyLitres().signum() <= 0)
            throw rule("FUEL_CARD_RESTRICTION_INVALID");
        try { Currency.getInstance(x.currency()); } catch (RuntimeException ex) { throw rule("FUEL_CARD_RESTRICTION_INVALID"); }
        Restriction result = cards.replaceRestriction(c.tenantId(), id, x, c.actorId(), OffsetDateTime.now(clock));
        cards.save(new FuelCard(card.id(), card.tenantId(), card.providerId(), card.alias(), card.providerCardReference(),
                card.maskedIdentifier(), card.lastFour(), card.expiryMonth(), card.expiryYear(), card.status(),
                card.version() + 1, card.createdBy(), card.createdAt(), OffsetDateTime.now(clock)));
        cards.audit(c.tenantId(), id, null, "RESTRICTIONS_CHANGED", "SUCCESS", x.reason(), c.actorId(), OffsetDateTime.now(clock));
        return result;
    }
    @Override public List<History> history(UUID tenantId, UUID cardId, int page, int limit) {
        required(tenantId, cardId); return cards.history(tenantId, cardId, Math.max(0, page), Math.min(100, Math.max(1, limit)));
    }
    private FuelCard required(UUID tenantId, UUID id) {
        return cards.find(tenantId, id).orElseThrow(() -> new NotFoundException("FUEL_CARD_NOT_FOUND", "Fuel card not found"));
    }
    private static BusinessRuleException rule(String code) { return new BusinessRuleException(code, code); }
    private static ConflictException conflict(String code) { return new ConflictException(code, code); }
}
