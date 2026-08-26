package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelPurchase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseHistory;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import com.transportlogistics.app.fuel.domain.model.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FuelPurchaseUseCase {
    FuelPurchase create(Command command, String actor);
    FuelPurchase update(UUID id, Command command, String actor);
    FuelPurchase submit(UUID id, String actor);
    FuelPurchase approve(UUID id, String comment, String actor);
    FuelPurchase receive(UUID id, ReceiptCommand command, String actor);
    FuelPurchase reconcile(UUID id, ReconciliationCommand command, String actor);
    FuelPurchase cancel(UUID id, String reason, String actor);
    FuelPurchase get(UUID id);
    PageResult<FuelPurchase> search(SearchQuery query);
    List<FuelPurchaseHistory> history(UUID id);
    VendorReference vendor(UUID vendorId);

    record Command(UUID vendorId, UUID fuelStationId, String fuelType, LocalDate purchaseDate,
                   String invoiceNumber, LocalDate invoiceDate, BigDecimal quantity, BigDecimal unitPrice,
                   BigDecimal taxRate, BigDecimal otherCharges, String currencyCode, String notes) {
    }

    record ReceiptCommand(BigDecimal receivedQuantity, OffsetDateTime receivedAt, UUID destinationFuelStationId,
                          String deliveryNoteNumber, String remarks) {
    }

    record ReconciliationCommand(String reconciliationNotes, String referenceNumber) {
    }

    record SearchQuery(int page, int limit, String search, String purchaseNumber, String invoiceNumber,
                       UUID vendorId, String fuelType, FuelPurchaseStatus status,
                       ReconciliationStatus reconciliationStatus, LocalDate fromDate, LocalDate toDate) {
        public SearchQuery {
            page = Math.max(0, page);
            limit = Math.min(100, Math.max(1, limit));
        }
    }

    record PageResult<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
    }

    record VendorReference(UUID id, String code, String name, boolean active) {
    }
}
