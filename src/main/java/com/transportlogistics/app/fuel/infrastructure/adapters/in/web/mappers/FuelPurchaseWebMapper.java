package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.domain.model.FuelPurchase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseHistory;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelPriceResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelPurchaseHistoryResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelPurchaseResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuelPurchaseWebMapper {

    FuelPriceResponse toResponse(FuelPrice price);
    List<FuelPriceResponse> toFuelPriceResponseList(List<FuelPrice> prices);

    FuelPurchaseHistoryResponse toResponse(FuelPurchaseHistory history);
    List<FuelPurchaseHistoryResponse> toFuelPurchaseHistoryResponseList(List<FuelPurchaseHistory> histories);

    default FuelPurchaseResponse toResponse(FuelPurchase p, FuelPurchaseUseCase.VendorReference vendor) {
        if (p == null) return null;
        return new FuelPurchaseResponse(
                p.id(),
                p.purchaseNumber(),
                vendor,
                p.fuelStationId(),
                p.fuelType(),
                p.purchaseDate(),
                p.invoiceNumber(),
                p.invoiceDate(),
                p.quantity(),
                p.unitPrice(),
                p.subtotal(),
                p.taxRate(),
                p.taxAmount(),
                p.otherCharges(),
                p.totalAmount(),
                p.currencyCode(),
                p.status(),
                p.reconciliationStatus(),
                p.receivedQuantity(),
                p.quantityVariance(),
                p.expectedUnitPrice(),
                p.priceVariance(),
                p.destinationFuelStationId(),
                p.deliveryNoteNumber(),
                p.receivedAt(),
                p.approvedBy(),
                p.approvedAt(),
                p.reconciledBy(),
                p.reconciledAt(),
                p.reconciliationNotes(),
                p.reconciliationReference(),
                p.notes(),
                p.createdBy(),
                p.createdAt(),
                p.updatedAt()
        );
    }
}
