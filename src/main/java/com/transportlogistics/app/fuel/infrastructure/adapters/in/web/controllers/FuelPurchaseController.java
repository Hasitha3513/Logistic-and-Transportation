package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelPurchaseWebMapper;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class FuelPurchaseController {

    private final FuelPurchaseUseCase purchases;
    private final FuelPriceUseCase prices;
    private final FuelPurchaseWebMapper mapper;

    public FuelPurchaseController(FuelPurchaseUseCase purchases, FuelPriceUseCase prices, FuelPurchaseWebMapper mapper) {
        this.purchases = purchases;
        this.prices = prices;
        this.mapper = mapper;
    }

    @GetMapping("/fuel-purchases")
    public PageResponse<FuelPurchaseResponse> search(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int limit,
                                                     @RequestParam(required = false) String search,
                                                     @RequestParam(required = false) String purchaseNumber,
                                                     @RequestParam(required = false) String invoiceNumber,
                                                     @RequestParam(required = false) UUID vendorId,
                                                     @RequestParam(required = false) String fuelType,
                                                     @RequestParam(required = false) FuelPurchaseStatus status,
                                                     @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
                                                     @RequestParam(required = false) LocalDate fromDate,
                                                     @RequestParam(required = false) LocalDate toDate) {
        var result = purchases.search(new FuelPurchaseUseCase.SearchQuery(page, limit, search, purchaseNumber,
                invoiceNumber, vendorId, fuelType, status, reconciliationStatus, fromDate, toDate));
        return new PageResponse<>(result.content().stream().map(this::response).toList(), result.page(),
                result.limit(), result.totalElements(), result.totalPages());
    }

    @PostMapping("/fuel-purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public FuelPurchaseResponse create(@Valid @RequestBody FuelPurchaseRequest request, Principal principal) {
        return response(purchases.create(request.command(), actor(principal)));
    }

    @GetMapping("/fuel-purchases/{id}")
    public FuelPurchaseResponse get(@PathVariable UUID id) {
        return response(purchases.get(id));
    }

    @PutMapping("/fuel-purchases/{id}")
    public FuelPurchaseResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody FuelPurchaseRequest request,
                                       Principal principal) {
        return response(purchases.update(id, request.command(), actor(principal)));
    }

    @PostMapping("/fuel-purchases/{id}/submit")
    public FuelPurchaseResponse submit(@PathVariable UUID id, Principal p) {
        return response(purchases.submit(id, actor(p)));
    }

    @PostMapping("/fuel-purchases/{id}/approve")
    public FuelPurchaseResponse approve(@PathVariable UUID id,
                                        @RequestBody(required = false) ApprovalRequest r,
                                        Principal p) {
        return response(purchases.approve(id, r == null ? null : r.comment(), actor(p)));
    }

    @PostMapping("/fuel-purchases/{id}/receive")
    public FuelPurchaseResponse receive(@PathVariable UUID id,
                                        @Valid @RequestBody ReceiptRequest r,
                                        Principal p) {
        return response(purchases.receive(id, r.command(), actor(p)));
    }

    @PostMapping("/fuel-purchases/{id}/reconcile")
    public FuelPurchaseResponse reconcile(@PathVariable UUID id,
                                          @Valid @RequestBody ReconciliationRequest r,
                                          Principal p) {
        return response(purchases.reconcile(id, r.command(), actor(p)));
    }

    @PostMapping("/fuel-purchases/{id}/cancel")
    public FuelPurchaseResponse cancel(@PathVariable UUID id,
                                       @Valid @RequestBody CancellationRequest r,
                                       Principal p) {
        return response(purchases.cancel(id, r.reason(), actor(p)));
    }

    @GetMapping("/fuel-purchases/{id}/history")
    public List<FuelPurchaseHistoryResponse> history(@PathVariable UUID id) {
        return mapper.toFuelPurchaseHistoryResponseList(purchases.history(id));
    }

    @GetMapping("/fuel-prices")
    public List<FuelPriceResponse> prices(@RequestParam(required = false) UUID vendorId,
                                          @RequestParam(required = false) String fuelType,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(required = false) LocalDate effectiveOn) {
        return mapper.toFuelPriceResponseList(prices.list(vendorId, fuelType, active, effectiveOn));
    }

    @PostMapping("/fuel-prices")
    @ResponseStatus(HttpStatus.CREATED)
    public FuelPriceResponse createPrice(@Valid @RequestBody FuelPriceRequest r) {
        return mapper.toResponse(prices.create(r.command()));
    }

    @PutMapping("/fuel-prices/{id}")
    public FuelPriceResponse updatePrice(@PathVariable UUID id, @Valid @RequestBody FuelPriceRequest r) {
        return mapper.toResponse(prices.update(id, r.command()));
    }

    private FuelPurchaseResponse response(FuelPurchase p) {
        var vendor = purchases.vendor(p.vendorId());
        return mapper.toResponse(p, vendor);
    }

    private String actor(Principal p) {
        return PrincipalUtils.resolveActorName(p, null);
    }
}
