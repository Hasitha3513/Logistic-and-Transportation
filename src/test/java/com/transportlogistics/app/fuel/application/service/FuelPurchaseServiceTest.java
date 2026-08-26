package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelPurchaseReceived;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.domain.policy.BunkerTankPolicy;
import com.transportlogistics.app.fuel.domain.service.FuelPurchasePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FuelPurchaseServiceTest {
    private final UUID vendorId = UUID.randomUUID(), actorId = UUID.randomUUID(), internalStationId = UUID.randomUUID(), externalStationId = UUID.randomUUID();
    private FuelPurchaseRepository purchases;
    private FuelPurchaseHistoryRepository history;
    private FuelStationRepository stations;
    private FuelVendorPort vendors;
    private FuelEventPublisher events;
    private BunkerTankRepository bunkerTanks;
    private BunkerStockLedgerRepository bunkerMovements;
    private FuelPurchaseService service;

    @BeforeEach
    void setUp() {
        purchases = mock(FuelPurchaseRepository.class);
        history = mock(FuelPurchaseHistoryRepository.class);
        var prices = mock(FuelPriceRepository.class);
        stations = mock(FuelStationRepository.class);
        vendors = mock(FuelVendorPort.class);
        var actors = mock(FuelActorPort.class);
        var numbers = mock(FuelPurchaseNumberGenerator.class);
        var tx = mock(FuelTransaction.class);
        events = mock(FuelEventPublisher.class);
        bunkerTanks = mock(BunkerTankRepository.class);
        bunkerMovements = mock(BunkerStockLedgerRepository.class);

        when(tx.execute(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        when(actors.find("manager")).thenReturn(Optional.of(new FuelActorPort.Actor(actorId, "manager")));
        when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId, "V1", "Vendor", true)));
        when(numbers.next(any())).thenReturn("FP-2026-000001");
        when(prices.findEffective(any(), any(), any())).thenReturn(Optional.of(new FuelPrice(UUID.randomUUID(), vendorId, "DIESEL", LocalDate.of(2026, 1, 1), null, new BigDecimal("9.50"), "LKR", true, time(), time())));
        when(purchases.save(any())).thenAnswer(i -> i.getArgument(0));
        when(history.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bunkerTanks.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bunkerMovements.save(any())).thenAnswer(i -> i.getArgument(0));

        when(stations.findById(internalStationId)).thenReturn(Optional.of(new FuelStation(internalStationId, "FS-INT", "Internal Station", FuelStationType.INTERNAL, true, null, null)));
        when(stations.findById(externalStationId)).thenReturn(Optional.of(new FuelStation(externalStationId, "FS-EXT", "External Station", FuelStationType.EXTERNAL, true, null, null)));

        service = new FuelPurchaseService(purchases, history, prices, stations, vendors, actors, numbers, tx, events,
                new FuelPurchasePolicy(), bunkerTanks, bunkerMovements, new BunkerTankPolicy(), Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsDraftWithAuthoritativeTotalsVarianceAndHistory() {
        var created = service.create(command(), "manager");
        assertEquals("FP-2026-000001", created.purchaseNumber());
        assertEquals(new BigDecimal("100.00"), created.subtotal());
        assertEquals(new BigDecimal("15.00"), created.taxAmount());
        assertEquals(new BigDecimal("117.00"), created.totalAmount());
        assertEquals(new BigDecimal("0.50"), created.priceVariance());
        verify(history).save(argThat(h -> h.action().equals("CREATED")));
    }

    @Test
    void rejectsMissingInactiveAndDuplicateVendorInvoice() {
        when(vendors.find(vendorId)).thenReturn(Optional.empty());
        assertCode("FUEL_VENDOR_NOT_FOUND", () -> service.create(command(), "manager"));
        when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId, "V1", "Vendor", false)));
        assertCode("FUEL_VENDOR_INACTIVE", () -> service.create(command(), "manager"));
        when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId, "V1", "Vendor", true)));
        when(purchases.existsByVendorAndInvoice(any(), any(), any())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(command(), "manager"));
    }

    @Test
    void followsLifecycleCalculatesReceiptVarianceAndWritesHistory() {
        var draft = purchase(FuelPurchaseStatus.DRAFT);
        var submitted = with(draft, FuelPurchaseStatus.SUBMITTED);
        var approved = with(draft, FuelPurchaseStatus.APPROVED);
        var received = with(draft, FuelPurchaseStatus.RECEIVED);
        when(purchases.findByIdForUpdate(draft.id())).thenReturn(Optional.of(draft), Optional.of(submitted), Optional.of(approved), Optional.of(received));
        assertEquals(FuelPurchaseStatus.SUBMITTED, service.submit(draft.id(), "manager").status());
        assertEquals(FuelPurchaseStatus.APPROVED, service.approve(draft.id(), "ok", "manager").status());
        var receipt = service.receive(draft.id(), new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("9.5"), time(), null, "DN1", null), "manager");
        assertEquals(new BigDecimal("-0.5000"), receipt.quantityVariance());
        assertEquals(FuelPurchaseStatus.RECONCILED, service.reconcile(draft.id(), new FuelPurchaseUseCase.ReconciliationCommand("accepted", "R1"), "manager").status());
        verify(history, times(4)).save(any());
        verify(events, times(3)).publish(any());
    }

    @Test
    void rejectsEditingReconciledAndCancellationAfterReceipt() {
        var p = purchase(FuelPurchaseStatus.RECONCILED);
        when(purchases.findByIdForUpdate(p.id())).thenReturn(Optional.of(p));
        assertThrows(ConflictException.class, () -> service.update(p.id(), command(), "manager"));
        var received = with(p, FuelPurchaseStatus.RECEIVED);
        when(purchases.findByIdForUpdate(p.id())).thenReturn(Optional.of(received));
        assertThrows(ConflictException.class, () -> service.cancel(p.id(), "reason", "manager"));
    }

    @Test
    void creditsBunkerStockWhenInternalPurchaseIsReceived() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));
        var tank = new BunkerTank(UUID.randomUUID(), internalStationId, "BT-01", "Tank 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, null, true, time(), time());
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(internalStationId, "DIESEL")).thenReturn(Optional.of(tank));

        var receipt = service.receive(approved.id(), new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("4800.000"), time(), internalStationId, "DN-101", "Received into bunker"), "manager");

        assertEquals(FuelPurchaseStatus.RECEIVED, receipt.status());
        assertEquals(new BigDecimal("4800.000"), receipt.receivedQuantity());
        verify(bunkerTanks).save(argThat(t -> t.currentStockLiters().compareTo(new BigDecimal("6800.000")) == 0));
        verify(bunkerMovements).save(argThat(m ->
                m.movementType() == BunkerMovementType.PURCHASE_RECEIPT &&
                m.referenceType() == BunkerReferenceType.FUEL_PURCHASE &&
                m.referenceId().equals(approved.id()) &&
                m.quantityLiters().compareTo(new BigDecimal("4800.000")) == 0 &&
                m.resultingBalanceLiters().compareTo(new BigDecimal("6800.000")) == 0));
    }

    @Test
    void rejectsInternalPurchaseWhenNoActiveBunkerTank() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(internalStationId, "DIESEL")).thenReturn(Optional.empty());

        assertCode("NO_ACTIVE_BUNKER_TANK", () -> service.receive(approved.id(),
                new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("1000.000"), time(), internalStationId, "DN-1", null), "manager"));
        verify(bunkerTanks, never()).save(any());
        verify(bunkerMovements, never()).save(any());
    }

    @Test
    void rejectsReceiptWhenTankCapacityWouldBeExceeded() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));
        var tank = new BunkerTank(UUID.randomUUID(), internalStationId, "BT-01", "Tank 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("8000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, null, true, time(), time());
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(internalStationId, "DIESEL")).thenReturn(Optional.of(tank));

        assertCode("BUNKER_CAPACITY_EXCEEDED", () -> service.receive(approved.id(),
                new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("3000.000"), time(), internalStationId, "DN-1", null), "manager"));
        verify(bunkerTanks, never()).save(any());
        verify(bunkerMovements, never()).save(any());
    }

    @Test
    void doesNotCreditBunkerStockForExternalStation() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));

        var receipt = service.receive(approved.id(), new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("500.000"), time(), externalStationId, "DN-EXT", null), "manager");

        assertEquals(FuelPurchaseStatus.RECEIVED, receipt.status());
        verify(bunkerTanks, never()).findActiveByStationAndFuelTypeForUpdate(any(), any());
        verify(bunkerTanks, never()).save(any());
        verify(bunkerMovements, never()).save(any());
    }

    @Test
    void preventsDuplicatePurchaseReceiptStockCredit() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));
        var tank = new BunkerTank(UUID.randomUUID(), internalStationId, "BT-01", "Tank 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, null, true, time(), time());
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(internalStationId, "DIESEL")).thenReturn(Optional.of(tank));
        when(bunkerMovements.existsByTankIdAndReference(tank.id(), BunkerReferenceType.FUEL_PURCHASE, approved.id())).thenReturn(true);

        service.receive(approved.id(), new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("1000.000"), time(), internalStationId, "DN-DUP", null), "manager");

        verify(bunkerTanks, never()).save(any());
        verify(bunkerMovements, never()).save(any());
    }

    @Test
    void rollsBackWhenBunkerMovementPersistenceFails() {
        var approved = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.findByIdForUpdate(approved.id())).thenReturn(Optional.of(approved));
        var tank = new BunkerTank(UUID.randomUUID(), internalStationId, "BT-01", "Tank 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, null, true, time(), time());
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(internalStationId, "DIESEL")).thenReturn(Optional.of(tank));
        when(bunkerMovements.existsByTankIdAndReference(any(), any(), any())).thenReturn(false);
        when(bunkerMovements.save(any())).thenThrow(new RuntimeException("Database error saving ledger movement"));

        assertThrows(RuntimeException.class, () -> service.receive(approved.id(), new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("1000.000"), time(), internalStationId, "DN-FAIL", null), "manager"));
        verify(purchases, never()).save(argThat(p -> p.status() == FuelPurchaseStatus.RECEIVED));
        verify(events, never()).publish(any(FuelPurchaseReceived.class));
    }

    private FuelPurchaseUseCase.Command command() {
        return new FuelPurchaseUseCase.Command(vendorId, null, "diesel", LocalDate.of(2026, 8, 16), "INV-1", LocalDate.of(2026, 8, 16), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("2"), "lkr", "notes");
    }

    private FuelPurchase purchase(FuelPurchaseStatus status) {
        var now = time();
        return new FuelPurchase(UUID.randomUUID(), "FP-2026-000001", vendorId, null, "DIESEL", LocalDate.of(2026, 8, 16), "INV-1", LocalDate.of(2026, 8, 16), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("100.00"), new BigDecimal("15"), new BigDecimal("15.00"), new BigDecimal("2.00"), new BigDecimal("117.00"), "LKR", status, ReconciliationStatus.PENDING, null, null, new BigDecimal("9.50"), new BigDecimal("0.50"), null, null, null, status == FuelPurchaseStatus.APPROVED ? actorId : null, status == FuelPurchaseStatus.APPROVED ? now : null, null, null, null, null, "notes", actorId, now, now);
    }

    private FuelPurchase with(FuelPurchase p, FuelPurchaseStatus status) {
        return new FuelPurchase(p.id(), p.purchaseNumber(), p.vendorId(), p.fuelStationId(), p.fuelType(), p.purchaseDate(), p.invoiceNumber(), p.invoiceDate(), p.quantity(), p.unitPrice(), p.subtotal(), p.taxRate(), p.taxAmount(), p.otherCharges(), p.totalAmount(), p.currencyCode(), status, ReconciliationStatus.PENDING, status == FuelPurchaseStatus.RECEIVED ? new BigDecimal("9.5") : null, status == FuelPurchaseStatus.RECEIVED ? new BigDecimal("-0.5") : null, p.expectedUnitPrice(), p.priceVariance(), null, null, status == FuelPurchaseStatus.RECEIVED ? time() : null, status == FuelPurchaseStatus.APPROVED ? actorId : null, status == FuelPurchaseStatus.APPROVED ? time() : null, null, null, null, null, p.notes(), p.createdBy(), p.createdAt(), p.updatedAt());
    }

    private OffsetDateTime time() {
        return OffsetDateTime.parse("2026-08-16T00:00:00Z");
    }

    private void assertCode(String code, Runnable action) {
        var ex = assertThrows(BusinessRuleException.class, action::run);
        assertEquals(code, ex.code());
    }
}
