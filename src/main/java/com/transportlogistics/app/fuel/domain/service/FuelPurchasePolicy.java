package com.transportlogistics.app.fuel.domain.service;

import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.domain.model.FuelPurchase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FuelPurchasePolicy {
    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 4;

    public CalculatedAmounts calculate(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate,
                                       BigDecimal otherCharges) {
        if (quantity == null || quantity.signum() <= 0) {
            validation("INVALID_FUEL_PURCHASE_QUANTITY", "Fuel purchase quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            validation("INVALID_FUEL_UNIT_PRICE", "Fuel unit price must be greater than zero");
        }
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;
        BigDecimal charges = otherCharges == null ? BigDecimal.ZERO : otherCharges;
        if (rate.signum() < 0) validation("INVALID_FUEL_TAX_RATE", "Tax rate cannot be negative");
        if (charges.signum() < 0) validation("INVALID_FUEL_OTHER_CHARGES", "Other charges cannot be negative");
        BigDecimal subtotal = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(rate).divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).add(charges).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new CalculatedAmounts(subtotal, rate, tax, charges.setScale(MONEY_SCALE, RoundingMode.HALF_UP), total);
    }

    public void validateDraft(FuelPurchase purchase) {
        calculate(purchase.quantity(), purchase.unitPrice(), purchase.taxRate(), purchase.otherCharges());
        if (purchase.vendorId() == null) validation("FUEL_VENDOR_NOT_FOUND", "Vendor is required");
        if (purchase.purchaseDate() == null) validation("FUEL_PURCHASE_DATE_REQUIRED", "Purchase date is required");
        if (purchase.fuelType() == null || purchase.fuelType().isBlank()) validation("FUEL_TYPE_REQUIRED", "Fuel type is required");
        if (purchase.currencyCode() == null || purchase.currencyCode().isBlank()) validation("FUEL_CURRENCY_REQUIRED", "Currency is required");
        if (purchase.invoiceNumber() != null && !purchase.invoiceNumber().isBlank() && purchase.invoiceDate() == null) {
            validation("FUEL_INVOICE_DATE_REQUIRED", "Invoice date is required when an invoice number is supplied");
        }
    }

    public void validatePrice(FuelPrice price) {
        if (price.unitPrice() == null || price.unitPrice().signum() <= 0) validation("INVALID_FUEL_UNIT_PRICE", "Fuel price must be greater than zero");
        if (price.effectiveFrom() == null) validation("FUEL_PRICE_EFFECTIVE_FROM_REQUIRED", "Effective-from date is required");
        if (price.effectiveTo() != null && !price.effectiveTo().isAfter(price.effectiveFrom())) {
            validation("INVALID_FUEL_PRICE_PERIOD", "Effective-to date must be after effective-from date");
        }
    }

    public void requireEditable(FuelPurchase purchase) { require(purchase, FuelPurchaseStatus.DRAFT, "FUEL_PURCHASE_NOT_EDITABLE", "Only a DRAFT fuel purchase can be edited"); }
    public void requireSubmittable(FuelPurchase purchase) { require(purchase, FuelPurchaseStatus.DRAFT, "FUEL_PURCHASE_NOT_SUBMITTABLE", "Submit requires a DRAFT fuel purchase"); }
    public void requireApprovable(FuelPurchase purchase) { require(purchase, FuelPurchaseStatus.SUBMITTED, "FUEL_PURCHASE_NOT_APPROVABLE", "Approve requires a SUBMITTED fuel purchase"); }
    public void requireReceivable(FuelPurchase purchase) { require(purchase, FuelPurchaseStatus.APPROVED, "FUEL_PURCHASE_NOT_RECEIVABLE", "Receive requires an APPROVED fuel purchase"); }
    public void requireReconcilable(FuelPurchase purchase) { require(purchase, FuelPurchaseStatus.RECEIVED, "FUEL_PURCHASE_NOT_RECONCILABLE", "Reconcile requires a RECEIVED fuel purchase"); }

    public void requireCancellable(FuelPurchase purchase, String reason) {
        if (purchase.status() != FuelPurchaseStatus.DRAFT && purchase.status() != FuelPurchaseStatus.SUBMITTED
                && purchase.status() != FuelPurchaseStatus.APPROVED) {
            conflict("FUEL_PURCHASE_NOT_CANCELLABLE", "Fuel purchase cannot be cancelled from status " + purchase.status());
        }
        if (reason == null || reason.isBlank()) validation("FUEL_PURCHASE_CANCELLATION_REASON_REQUIRED", "Cancellation reason is required");
    }

    private void require(FuelPurchase purchase, FuelPurchaseStatus status, String code, String message) {
        if (purchase.status() != status) conflict(code, message);
    }

    private void validation(String code, String message) { throw new BusinessRuleException(code, message); }
    private void conflict(String code, String message) { throw new ConflictException(code, message); }

    public record CalculatedAmounts(BigDecimal subtotal, BigDecimal taxRate, BigDecimal taxAmount,
                                    BigDecimal otherCharges, BigDecimal totalAmount) {
    }
}
