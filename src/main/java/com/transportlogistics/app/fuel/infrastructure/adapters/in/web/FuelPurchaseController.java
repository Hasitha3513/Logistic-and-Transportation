package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
class FuelPurchaseController {
    private final FuelPurchaseUseCase purchases;
    private final FuelPriceUseCase prices;

    FuelPurchaseController(FuelPurchaseUseCase purchases, FuelPriceUseCase prices) {
        this.purchases = purchases; this.prices = prices;
    }

    @GetMapping("/fuel-purchases")
    PageResponse<FuelPurchaseResponse> search(@RequestParam(defaultValue="0") int page,
                                              @RequestParam(defaultValue="20") int limit,
                                              @RequestParam(required=false) String search,
                                              @RequestParam(required=false) String purchaseNumber,
                                              @RequestParam(required=false) String invoiceNumber,
                                              @RequestParam(required=false) UUID vendorId,
                                              @RequestParam(required=false) String fuelType,
                                              @RequestParam(required=false) FuelPurchaseStatus status,
                                              @RequestParam(required=false) ReconciliationStatus reconciliationStatus,
                                              @RequestParam(required=false) LocalDate fromDate,
                                              @RequestParam(required=false) LocalDate toDate) {
        var result = purchases.search(new FuelPurchaseUseCase.SearchQuery(page, limit, search, purchaseNumber,
                invoiceNumber, vendorId, fuelType, status, reconciliationStatus, fromDate, toDate));
        return new PageResponse<>(result.content().stream().map(this::response).toList(), result.page(), result.limit(), result.totalElements(), result.totalPages());
    }

    @PostMapping("/fuel-purchases") @ResponseStatus(HttpStatus.CREATED)
    FuelPurchaseResponse create(@Valid @RequestBody FuelPurchaseRequest request, Principal principal) {
        return response(purchases.create(request.command(), actor(principal)));
    }

    @GetMapping("/fuel-purchases/{id}") FuelPurchaseResponse get(@PathVariable UUID id) { return response(purchases.get(id)); }
    @PutMapping("/fuel-purchases/{id}") FuelPurchaseResponse update(@PathVariable UUID id, @Valid @RequestBody FuelPurchaseRequest request, Principal principal) { return response(purchases.update(id, request.command(), actor(principal))); }
    @PostMapping("/fuel-purchases/{id}/submit") FuelPurchaseResponse submit(@PathVariable UUID id, Principal p) { return response(purchases.submit(id, actor(p))); }
    @PostMapping("/fuel-purchases/{id}/approve") FuelPurchaseResponse approve(@PathVariable UUID id, @RequestBody(required=false) ApprovalRequest r, Principal p) { return response(purchases.approve(id, r == null ? null : r.comment(), actor(p))); }
    @PostMapping("/fuel-purchases/{id}/receive") FuelPurchaseResponse receive(@PathVariable UUID id, @Valid @RequestBody ReceiptRequest r, Principal p) { return response(purchases.receive(id, r.command(), actor(p))); }
    @PostMapping("/fuel-purchases/{id}/reconcile") FuelPurchaseResponse reconcile(@PathVariable UUID id, @Valid @RequestBody ReconciliationRequest r, Principal p) { return response(purchases.reconcile(id, r.command(), actor(p))); }
    @PostMapping("/fuel-purchases/{id}/cancel") FuelPurchaseResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancellationRequest r, Principal p) { return response(purchases.cancel(id, r.reason(), actor(p))); }
    @GetMapping("/fuel-purchases/{id}/history") List<FuelPurchaseHistory> history(@PathVariable UUID id) { return purchases.history(id); }

    @GetMapping("/fuel-prices")
    List<FuelPrice> prices(@RequestParam(required=false) UUID vendorId, @RequestParam(required=false) String fuelType,
                           @RequestParam(required=false) Boolean active, @RequestParam(required=false) LocalDate effectiveOn) {
        return prices.list(vendorId, fuelType, active, effectiveOn);
    }
    @PostMapping("/fuel-prices") @ResponseStatus(HttpStatus.CREATED)
    FuelPrice createPrice(@Valid @RequestBody FuelPriceRequest r) { return prices.create(r.command()); }
    @PutMapping("/fuel-prices/{id}") FuelPrice updatePrice(@PathVariable UUID id, @Valid @RequestBody FuelPriceRequest r) { return prices.update(id, r.command()); }

    private FuelPurchaseResponse response(FuelPurchase p) {
        return new FuelPurchaseResponse(p.id(), p.purchaseNumber(), purchases.vendor(p.vendorId()), p.fuelStationId(),
                p.fuelType(), p.purchaseDate(), p.invoiceNumber(), p.invoiceDate(), p.quantity(), p.unitPrice(),
                p.subtotal(), p.taxRate(), p.taxAmount(), p.otherCharges(), p.totalAmount(), p.currencyCode(),
                p.status(), p.reconciliationStatus(), p.receivedQuantity(), p.quantityVariance(), p.expectedUnitPrice(),
                p.priceVariance(), p.destinationFuelStationId(), p.deliveryNoteNumber(), p.receivedAt(), p.approvedBy(),
                p.approvedAt(), p.reconciledBy(), p.reconciledAt(), p.reconciliationNotes(), p.reconciliationReference(),
                p.notes(), p.createdBy(), p.createdAt(), p.updatedAt());
    }
    private String actor(Principal p) { return p == null ? null : p.getName(); }

    record FuelPurchaseRequest(@NotNull UUID vendorId, UUID fuelStationId, @NotBlank @Size(max=40) String fuelType,
                               @NotNull LocalDate purchaseDate, @Size(max=100) String invoiceNumber, LocalDate invoiceDate,
                               @NotNull @DecimalMin("0.0001") BigDecimal quantity,
                               @NotNull @DecimalMin(value="0.0001") BigDecimal unitPrice,
                               @DecimalMin("0.0") BigDecimal taxRate, @DecimalMin("0.0") BigDecimal otherCharges,
                               @NotBlank @Pattern(regexp="[A-Za-z]{3}") String currencyCode,
                               @Size(max=1000) String notes) {
        FuelPurchaseUseCase.Command command() { return new FuelPurchaseUseCase.Command(vendorId, fuelStationId, fuelType, purchaseDate, invoiceNumber, invoiceDate, quantity, unitPrice, taxRate, otherCharges, currencyCode, notes); }
    }
    record ApprovalRequest(@Size(max=1000) String comment) {}
    record ReceiptRequest(@NotNull @DecimalMin("0.0001") BigDecimal receivedQuantity, OffsetDateTime receivedAt,
                          UUID destinationFuelStationId, @Size(max=100) String deliveryNoteNumber,
                          @Size(max=1000) String remarks) {
        FuelPurchaseUseCase.ReceiptCommand command() { return new FuelPurchaseUseCase.ReceiptCommand(receivedQuantity, receivedAt, destinationFuelStationId, deliveryNoteNumber, remarks); }
    }
    record ReconciliationRequest(@Size(max=1000) String reconciliationNotes, @Size(max=100) String referenceNumber) {
        FuelPurchaseUseCase.ReconciliationCommand command() { return new FuelPurchaseUseCase.ReconciliationCommand(reconciliationNotes, referenceNumber); }
    }
    record CancellationRequest(@NotBlank @Size(max=1000) String reason) {}
    record FuelPriceRequest(@NotNull UUID vendorId, @NotBlank @Size(max=40) String fuelType,
                            @NotNull LocalDate effectiveFrom, LocalDate effectiveTo,
                            @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
                            @NotBlank @Pattern(regexp="[A-Za-z]{3}") String currencyCode, Boolean active) {
        FuelPriceUseCase.Command command() { return new FuelPriceUseCase.Command(vendorId, fuelType, effectiveFrom, effectiveTo, unitPrice, currencyCode, active); }
    }
    record FuelPurchaseResponse(UUID id, String purchaseNumber, FuelPurchaseUseCase.VendorReference vendor,
                                UUID fuelStationId, String fuelType, LocalDate purchaseDate, String invoiceNumber,
                                LocalDate invoiceDate, BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal,
                                BigDecimal taxRate, BigDecimal taxAmount, BigDecimal otherCharges, BigDecimal totalAmount,
                                String currencyCode, FuelPurchaseStatus status, ReconciliationStatus reconciliationStatus,
                                BigDecimal receivedQuantity, BigDecimal quantityVariance, BigDecimal expectedUnitPrice,
                                BigDecimal priceVariance, UUID destinationFuelStationId, String deliveryNoteNumber,
                                OffsetDateTime receivedAt, UUID approvedBy, OffsetDateTime approvedAt, UUID reconciledBy,
                                OffsetDateTime reconciledAt, String reconciliationNotes, String reconciliationReference, String notes,
                                UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    record PageResponse<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {}
}
