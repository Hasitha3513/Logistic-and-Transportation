package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.DipReading;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.BunkerWebMapper;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class BunkerTankController {

    private final BunkerTankUseCase bunkerTanks;
    private final BunkerWebMapper mapper;

    public BunkerTankController(BunkerTankUseCase bunkerTanks, BunkerWebMapper mapper) {
        this.bunkerTanks = bunkerTanks;
        this.mapper = mapper;
    }

    @GetMapping("/bunker-tanks")
    public List<BunkerTankResponse> list(
            @RequestParam(required = false) UUID fuelStationId,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) Boolean active
    ) {
        return bunkerTanks.list(fuelStationId, fuelType, active).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/bunker-tanks")
    @ResponseStatus(HttpStatus.CREATED)
    public BunkerTankResponse create(@Valid @RequestBody BunkerTankCreateRequest request, Principal principal) {
        var created = bunkerTanks.create(mapper.toCommand(request), actor(principal));
        return mapper.toResponse(created);
    }

    @GetMapping("/bunker-tanks/{id}")
    public BunkerTankResponse get(@PathVariable UUID id) {
        return mapper.toResponse(bunkerTanks.get(id));
    }

    @PutMapping("/bunker-tanks/{id}")
    public BunkerTankResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BunkerTankUpdateRequest request,
            Principal principal
    ) {
        var updated = bunkerTanks.update(id, mapper.toCommand(request), actor(principal));
        return mapper.toResponse(updated);
    }

    @GetMapping("/bunker-tanks/{id}/balance")
    public BunkerTankBalanceResponse getBalance(@PathVariable UUID id) {
        var tank = bunkerTanks.get(id);
        var dipReadings = bunkerTanks.listDipReadings(id);
        DipReading latestDip = dipReadings.isEmpty() ? null : dipReadings.get(0);

        String stockStatus = "NORMAL";
        if (!tank.active() || tank.status() != com.transportlogistics.app.fuel.domain.model.BunkerTankStatus.ACTIVE) {
            stockStatus = "OUT_OF_SERVICE";
        } else if (tank.isLowStock()) {
            stockStatus = "LOW_STOCK";
        } else if (tank.currentStockLiters().compareTo(tank.capacityLiters().multiply(new java.math.BigDecimal("0.95"))) >= 0) {
            stockStatus = "NEAR_CAPACITY";
        }

        return new BunkerTankBalanceResponse(
                tank.id(),
                tank.fuelStationId(),
                tank.tankCode(),
                tank.tankName(),
                tank.fuelType(),
                tank.capacityLiters(),
                tank.currentStockLiters(),
                tank.availableCapacity(),
                tank.minimumStockLiters(),
                tank.status(),
                stockStatus,
                latestDip != null ? latestDip.physicalQuantityLiters() : null,
                latestDip != null ? latestDip.measuredAt() : null,
                latestDip != null ? latestDip.varianceQuantityLiters() : null
        );
    }

    @PostMapping("/bunker-tanks/{id}/opening-balance")
    public BunkerTankResponse setOpeningBalance(
            @PathVariable UUID id,
            @Valid @RequestBody BunkerOpeningBalanceRequest request,
            Principal principal
    ) {
        var updated = bunkerTanks.setOpeningBalance(id, request.openingBalanceLiters(), request.reason(), actor(principal));
        return mapper.toResponse(updated);
    }

    @GetMapping("/bunker-tanks/{id}/movements")
    public PageResponse<BunkerStockMovementResponse> listMovements(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        var items = bunkerTanks.listMovements(id, page, limit).stream().map(mapper::toResponse).toList();
        long total = bunkerTanks.countMovements(id);
        int totalPages = limit > 0 ? (int) Math.ceil((double) total / limit) : 0;
        return new PageResponse<>(items, page, limit, total, totalPages);
    }

    @PostMapping("/bunker-tanks/{id}/dip-readings")
    @ResponseStatus(HttpStatus.CREATED)
    public DipReadingResponse recordDipReading(
            @PathVariable UUID id,
            @Valid @RequestBody DipReadingRequest request,
            Principal principal
    ) {
        var reading = bunkerTanks.recordDipReading(id, request.physicalQuantityLiters(), request.notes(), actor(principal));
        return mapper.toResponse(reading);
    }

    @GetMapping("/bunker-tanks/{id}/dip-readings")
    public List<DipReadingResponse> listDipReadings(@PathVariable UUID id) {
        return bunkerTanks.listDipReadings(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/bunker-tanks/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public StockAdjustmentResponse adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Principal principal
    ) {
        var adjustment = bunkerTanks.adjustStock(
                id,
                request.quantityDeltaLiters(),
                request.reason(),
                request.sourceDipReadingId(),
                actor(principal)
        );
        return mapper.toResponse(adjustment);
    }

    @PostMapping("/bunker-transfers")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> transfer(@Valid @RequestBody BunkerTransferRequest request, Principal principal) {
        bunkerTanks.transfer(mapper.toCommand(request), actor(principal));
        return ResponseEntity.ok().build();
    }

    private String actor(Principal principal) {
        return PrincipalUtils.resolveActorName(principal, null);
    }
}
