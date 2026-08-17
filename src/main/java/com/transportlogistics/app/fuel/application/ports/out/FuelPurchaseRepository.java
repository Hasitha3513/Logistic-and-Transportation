package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchase;

import java.util.Optional;
import java.util.UUID;

public interface FuelPurchaseRepository {
    FuelPurchase save(FuelPurchase purchase);
    Optional<FuelPurchase> findById(UUID id);
    Optional<FuelPurchase> findByIdForUpdate(UUID id);
    FuelPurchaseUseCase.PageResult<FuelPurchase> search(FuelPurchaseUseCase.SearchQuery query);
    boolean existsByPurchaseNumber(String purchaseNumber);
    boolean existsByVendorAndInvoice(UUID vendorId, String invoiceNumber, UUID excludingId);
}
