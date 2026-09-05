package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelPurchaseApproved;
import com.transportlogistics.app.fuel.FuelPurchaseCancelled;
import com.transportlogistics.app.fuel.FuelPurchaseReceived;
import com.transportlogistics.app.fuel.FuelPurchaseReconciled;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.domain.policy.BunkerTankPolicy;
import com.transportlogistics.app.fuel.domain.service.FuelPurchasePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FuelPurchaseService implements FuelPurchaseUseCase {
    private final FuelPurchaseRepository purchases;
    private final FuelPurchaseHistoryRepository history;
    private final FuelPriceRepository prices;
    private final FuelStationRepository stations;
    private final FuelVendorPort vendors;
    private final FuelActorPort actors;
    private final FuelPurchaseNumberGenerator numbers;
    private final FuelTransaction transactions;
    private final FuelEventPublisher events;
    private final FuelPurchasePolicy policy;
    private final BunkerTankRepository bunkerTanks;
    private final BunkerStockLedgerRepository bunkerMovements;
    private final BunkerTankPolicy bunkerTankPolicy;
    private final Clock clock;

    public FuelPurchaseService(FuelPurchaseRepository purchases, FuelPurchaseHistoryRepository history,
                               FuelPriceRepository prices, FuelStationRepository stations, FuelVendorPort vendors,
                               FuelActorPort actors, FuelPurchaseNumberGenerator numbers, FuelTransaction transactions,
                               FuelEventPublisher events, FuelPurchasePolicy policy,
                               BunkerTankRepository bunkerTanks, BunkerStockLedgerRepository bunkerMovements,
                               BunkerTankPolicy bunkerTankPolicy, Clock clock) {
        this.purchases = purchases; this.history = history; this.prices = prices; this.stations = stations;
        this.vendors = vendors; this.actors = actors; this.numbers = numbers; this.transactions = transactions;
        this.events = events; this.policy = policy;
        this.bunkerTanks = bunkerTanks; this.bunkerMovements = bunkerMovements;
        this.bunkerTankPolicy = bunkerTankPolicy != null ? bunkerTankPolicy : new BunkerTankPolicy();
        this.clock = clock;
    }

    public FuelPurchaseService(FuelPurchaseRepository purchases, FuelPurchaseHistoryRepository history,
                               FuelPriceRepository prices, FuelStationRepository stations, FuelVendorPort vendors,
                               FuelActorPort actors, FuelPurchaseNumberGenerator numbers, FuelTransaction transactions,
                               FuelEventPublisher events, FuelPurchasePolicy policy, Clock clock) {
        this(purchases, history, prices, stations, vendors, actors, numbers, transactions, events, policy, null, null, null, clock);
    }

    @Override
    public FuelPurchase create(Command command, String username) {
        return transactions.execute(() -> {
            var actor = actor(username); var now = OffsetDateTime.now(clock);
            var purchase = draft(UUID.randomUUID(), numbers.next(command.purchaseDate()), command, actor.id(), now, now);
            validateReferences(purchase); validateDuplicateInvoice(purchase, null);
            var saved = purchases.save(purchase); append(saved, null, saved.status(), "CREATED", actor, saved.notes(), now);
            return saved;
        });
    }

    @Override
    public FuelPurchase update(UUID id, Command command, String username) {
        return transactions.execute(() -> {
            var actor = actor(username); var current = locked(id); policy.requireEditable(current);
            var updated = draft(current.id(), current.purchaseNumber(), command, current.createdBy(), current.createdAt(), OffsetDateTime.now(clock));
            validateReferences(updated); validateDuplicateInvoice(updated, id);
            var saved = purchases.save(updated); append(saved, current.status(), saved.status(), "UPDATED", actor, saved.notes(), saved.updatedAt());
            return saved;
        });
    }

    @Override
    public FuelPurchase submit(UUID id, String username) {
        return transition(id, username, "SUBMITTED", null, (current, actor, now) -> {
            policy.requireSubmittable(current); validateReferences(current); validateInvoice(current); validateDuplicateInvoice(current, current.id());
            return copy(current, FuelPurchaseStatus.SUBMITTED, ReconciliationStatus.PENDING, current.receivedQuantity(), current.quantityVariance(),
                    current.destinationFuelStationId(), current.deliveryNoteNumber(), current.receivedAt(), current.approvedBy(), current.approvedAt(),
                    current.reconciledBy(), current.reconciledAt(), current.reconciliationNotes(), current.reconciliationReference(), now);
        });
    }

    @Override
    public FuelPurchase approve(UUID id, String comment, String username) {
        FuelPurchase approved = transition(id, username, "APPROVED", comment, (current, actor, now) -> {
            policy.requireApprovable(current); validateReferences(current); validateInvoice(current);
            return copy(current, FuelPurchaseStatus.APPROVED, ReconciliationStatus.PENDING, current.receivedQuantity(), current.quantityVariance(),
                    current.destinationFuelStationId(), current.deliveryNoteNumber(), current.receivedAt(), actor.id(), now,
                    current.reconciledBy(), current.reconciledAt(), current.reconciliationNotes(), current.reconciliationReference(), now);
        });
        events.publish(new FuelPurchaseApproved(approved.id(), approved.purchaseNumber(), approved.vendorId(),
                approved.totalAmount(), approved.currencyCode(), approved.approvedBy(), approved.approvedAt()));
        return approved;
    }

    @Override
    public FuelPurchase receive(UUID id, ReceiptCommand command, String username) {
        FuelPurchase received = transition(id, username, "RECEIVED", command.remarks(), (current, actor, now) -> {
            policy.requireReceivable(current);
            if (command.receivedQuantity() == null || command.receivedQuantity().signum() <= 0) throw new BusinessRuleException("INVALID_FUEL_PURCHASE_RECEIPT", "Received quantity must be greater than zero");
            OffsetDateTime receivedAt = command.receivedAt() == null ? now : command.receivedAt();
            if (receivedAt.isAfter(now.plusMinutes(5))) throw new BusinessRuleException("INVALID_FUEL_PURCHASE_RECEIPT", "Received time cannot be in the future");
            UUID destination = command.destinationFuelStationId() != null ? command.destinationFuelStationId() : current.destinationFuelStationId() != null ? current.destinationFuelStationId() : current.fuelStationId();
            FuelStation station = validateStation(destination);
            if (station != null && station.isInternal() && bunkerTanks != null) {
                var bunkerTank = bunkerTanks.findActiveByStationAndFuelTypeForUpdate(destination, current.fuelType())
                        .orElseThrow(() -> new BusinessRuleException("NO_ACTIVE_BUNKER_TANK", "No active bunker tank found for internal fuel station " + destination + " and fuel type " + current.fuelType()));
                boolean alreadyReceived = bunkerMovements != null && bunkerMovements.existsByTankIdAndReference(bunkerTank.id(), BunkerReferenceType.FUEL_PURCHASE, current.id());
                if (!alreadyReceived) {
                    bunkerTankPolicy.validateReceivable(bunkerTank, command.receivedQuantity(), current.fuelType());
                    BigDecimal newStock = bunkerTank.currentStockLiters().add(command.receivedQuantity()).setScale(BunkerTankPolicy.QUANTITY_SCALE, java.math.RoundingMode.HALF_UP);
                    bunkerTanks.save(bunkerTank.withStock(newStock));
                    if (bunkerMovements != null) {
                        bunkerMovements.save(new BunkerStockMovement(
                                UUID.randomUUID(),
                                bunkerTank.id(),
                                bunkerMovements.nextLedgerSequence(bunkerTank.id()),
                                BunkerMovementType.PURCHASE_RECEIPT,
                                command.receivedQuantity(),
                                newStock,
                                BunkerReferenceType.FUEL_PURCHASE,
                                current.id(),
                                receivedAt,
                                actor.id(),
                                "Fuel purchase receipt " + current.purchaseNumber() + (command.deliveryNoteNumber() != null ? " (DN: " + command.deliveryNoteNumber().trim() + ")" : ""),
                                now
                        ));
                    }
                }
            }
            BigDecimal variance = command.receivedQuantity().subtract(current.quantity()).setScale(FuelPurchasePolicy.QUANTITY_SCALE, java.math.RoundingMode.HALF_UP);
            return copy(current, FuelPurchaseStatus.RECEIVED, ReconciliationStatus.PENDING, command.receivedQuantity(), variance,
                    destination, trim(command.deliveryNoteNumber()), receivedAt, current.approvedBy(), current.approvedAt(),
                    current.reconciledBy(), current.reconciledAt(), current.reconciliationNotes(), current.reconciliationReference(), now);
        });
        events.publish(new FuelPurchaseReceived(received.id(), received.purchaseNumber(), received.vendorId(),
                received.destinationFuelStationId(), received.fuelType(), received.receivedQuantity(), received.quantityVariance(), received.receivedAt()));
        return received;
    }

    @Override
    public FuelPurchase reconcile(UUID id, ReconciliationCommand command, String username) {
        FuelPurchase reconciled = transition(id, username, "RECONCILED", command.reconciliationNotes(), (current, actor, now) -> {
            policy.requireReconcilable(current);
            if (current.receivedQuantity() == null || current.receivedAt() == null) throw new BusinessRuleException("FUEL_PURCHASE_NOT_RECONCILABLE", "Receipt information is incomplete");
            return copy(current, FuelPurchaseStatus.RECONCILED, ReconciliationStatus.RECONCILED, current.receivedQuantity(), current.quantityVariance(),
                    current.destinationFuelStationId(), current.deliveryNoteNumber(), current.receivedAt(), current.approvedBy(), current.approvedAt(),
                    actor.id(), now, trim(command.reconciliationNotes()), trim(command.referenceNumber()), now);
        });
        events.publish(new FuelPurchaseReconciled(reconciled.id(), reconciled.purchaseNumber(), reconciled.quantityVariance(),
                reconciled.priceVariance(), reconciled.reconciledBy(), reconciled.reconciledAt()));
        return reconciled;
    }

    @Override
    public FuelPurchase cancel(UUID id, String reason, String username) {
        FuelPurchase cancelled = transition(id, username, "CANCELLED", reason, (current, actor, now) -> {
            policy.requireCancellable(current, reason);
            return copy(current, FuelPurchaseStatus.CANCELLED, ReconciliationStatus.NOT_APPLICABLE, current.receivedQuantity(), current.quantityVariance(),
                    current.destinationFuelStationId(), current.deliveryNoteNumber(), current.receivedAt(), current.approvedBy(), current.approvedAt(),
                    current.reconciledBy(), current.reconciledAt(), current.reconciliationNotes(), current.reconciliationReference(), now);
        });
        events.publish(new FuelPurchaseCancelled(cancelled.id(), cancelled.purchaseNumber(), reason,
                actor(username).id(), cancelled.updatedAt()));
        return cancelled;
    }

    @Override public FuelPurchase get(UUID id) { return purchases.findById(id).orElseThrow(() -> notFound(id)); }
    @Override public PageResult<FuelPurchase> search(SearchQuery query) { return purchases.search(query); }
    @Override public List<FuelPurchaseHistory> history(UUID id) { get(id); return history.findByPurchaseId(id); }

    @Override
    public VendorReference vendor(UUID vendorId) {
        var vendor = vendors.find(vendorId).orElseThrow(() -> new NotFoundException("FUEL_VENDOR_NOT_FOUND", "Vendor not found: " + vendorId));
        return new VendorReference(vendor.id(), vendor.code(), vendor.name(), vendor.active());
    }

    private FuelPurchase draft(UUID id, String number, Command command, UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        String fuelType = normalize(command.fuelType()); String currency = normalizeCurrency(command.currencyCode());
        var amounts = policy.calculate(command.quantity(), command.unitPrice(), command.taxRate(), command.otherCharges());
        BigDecimal expected = prices.findEffective(command.vendorId(), fuelType, command.purchaseDate()).map(FuelPrice::unitPrice).orElse(null);
        BigDecimal priceVariance = expected == null ? null : command.unitPrice().subtract(expected).setScale(FuelPurchasePolicy.MONEY_SCALE, java.math.RoundingMode.HALF_UP);
        var purchase = new FuelPurchase(id, number, command.vendorId(), command.fuelStationId(), fuelType, command.purchaseDate(),
                trim(command.invoiceNumber()), command.invoiceDate(), command.quantity(), command.unitPrice(), amounts.subtotal(), amounts.taxRate(),
                amounts.taxAmount(), amounts.otherCharges(), amounts.totalAmount(), currency, FuelPurchaseStatus.DRAFT,
                ReconciliationStatus.PENDING, null, null, expected, priceVariance, null, null, null, null, null,
                null, null, null, null, trim(command.notes()), createdBy, createdAt, updatedAt);
        policy.validateDraft(purchase); return purchase;
    }

    private void validateReferences(FuelPurchase purchase) {
        var vendor = vendors.find(purchase.vendorId()).orElseThrow(() -> new BusinessRuleException("FUEL_VENDOR_NOT_FOUND", "Vendor not found: " + purchase.vendorId()));
        if (!vendor.active()) throw new BusinessRuleException("FUEL_VENDOR_INACTIVE", "Inactive vendor cannot be used for a new fuel purchase");
        validateStation(purchase.fuelStationId());
    }

    private FuelStation validateStation(UUID stationId) {
        if (stationId == null) return null;
        var station = stations.findById(stationId).orElseThrow(() -> new BusinessRuleException("FUEL_STATION_NOT_FOUND", "Fuel station not found: " + stationId));
        if (!station.active()) throw new BusinessRuleException("FUEL_STATION_INACTIVE", "Inactive fuel station cannot receive fuel");
        return station;
    }

    private void validateInvoice(FuelPurchase purchase) {
        if (purchase.invoiceNumber() == null || purchase.invoiceNumber().isBlank()) throw new BusinessRuleException("FUEL_INVOICE_REQUIRED", "Invoice number is required before submission");
        if (purchase.invoiceDate() == null) throw new BusinessRuleException("FUEL_INVOICE_DATE_REQUIRED", "Invoice date is required before submission");
    }

    private void validateDuplicateInvoice(FuelPurchase purchase, UUID excludingId) {
        if (purchase.invoiceNumber() != null && purchases.existsByVendorAndInvoice(purchase.vendorId(), purchase.invoiceNumber(), excludingId)) {
            throw new ConflictException("DUPLICATE_FUEL_INVOICE", "This vendor invoice has already been recorded");
        }
    }

    private FuelPurchase transition(UUID id, String username, String action, String comment, Transition transition) {
        return transactions.execute(() -> {
            var actor = actor(username); var current = locked(id); var now = OffsetDateTime.now(clock);
            var changed = transition.apply(current, actor, now); var saved = purchases.save(changed);
            append(saved, current.status(), saved.status(), action, actor, comment, now); return saved;
        });
    }

    private FuelPurchase copy(FuelPurchase p, FuelPurchaseStatus status, ReconciliationStatus reconciliationStatus,
                              BigDecimal receivedQuantity, BigDecimal quantityVariance, UUID destinationStationId,
                              String deliveryNote, OffsetDateTime receivedAt, UUID approvedBy, OffsetDateTime approvedAt,
                              UUID reconciledBy, OffsetDateTime reconciledAt, String reconciliationNotes,
                              String reconciliationReference, OffsetDateTime updatedAt) {
        return new FuelPurchase(p.id(), p.purchaseNumber(), p.vendorId(), p.fuelStationId(), p.fuelType(), p.purchaseDate(),
                p.invoiceNumber(), p.invoiceDate(), p.quantity(), p.unitPrice(), p.subtotal(), p.taxRate(), p.taxAmount(),
                p.otherCharges(), p.totalAmount(), p.currencyCode(), status, reconciliationStatus, receivedQuantity,
                quantityVariance, p.expectedUnitPrice(), p.priceVariance(), destinationStationId, deliveryNote, receivedAt,
                approvedBy, approvedAt, reconciledBy, reconciledAt, reconciliationNotes, reconciliationReference,
                p.notes(), p.createdBy(), p.createdAt(), updatedAt);
    }

    private void append(FuelPurchase purchase, FuelPurchaseStatus from, FuelPurchaseStatus to, String action,
                        FuelActorPort.Actor actor, String comment, OffsetDateTime at) {
        history.save(new FuelPurchaseHistory(UUID.randomUUID(), purchase.id(), from, to, action, actor.id(),
                actor.username(), trim(comment), purchase.quantityVariance(), purchase.priceVariance(), at));
    }

    private FuelActorPort.Actor actor(String username) {
        if (username == null || username.isBlank()) throw new BusinessRuleException("FUEL_ACTOR_REQUIRED", "An authenticated actor is required");
        return actors.find(username).orElseThrow(() -> new BusinessRuleException("FUEL_ACTOR_NOT_FOUND", "Authenticated user could not be resolved"));
    }
    private FuelPurchase locked(UUID id) { return purchases.findByIdForUpdate(id).orElseThrow(() -> notFound(id)); }
    private NotFoundException notFound(UUID id) { return new NotFoundException("FUEL_PURCHASE_NOT_FOUND", "Fuel purchase not found: " + id); }
    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String normalizeCurrency(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    @FunctionalInterface private interface Transition { FuelPurchase apply(FuelPurchase current, FuelActorPort.Actor actor, OffsetDateTime now); }
}
