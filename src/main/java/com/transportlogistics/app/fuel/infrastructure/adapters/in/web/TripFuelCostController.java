package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fuel.PricingSource;
import com.transportlogistics.app.fuel.TripFuelCost;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import com.transportlogistics.app.fuel.TripFuelCostLine;
import com.transportlogistics.app.fuel.application.ports.in.TripFuelCostUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/fuel-cost")
public class TripFuelCostController {

    private final TripFuelCostUseCase tripFuelCostUseCase;

    public TripFuelCostController(TripFuelCostUseCase tripFuelCostUseCase) {
        this.tripFuelCostUseCase = tripFuelCostUseCase;
    }

    @GetMapping
    public ResponseEntity<TripFuelCostResponse> getTripFuelCost(@PathVariable UUID tripId) {
        var cost = tripFuelCostUseCase.getTripFuelCost(tripId);
        return ResponseEntity.ok(mapResponse(cost));
    }

    private TripFuelCostResponse mapResponse(TripFuelCost cost) {
        var lines = cost.lines().stream().map(this::mapLine).toList();
        return new TripFuelCostResponse(
                cost.tripId(),
                cost.vehicleId(),
                cost.totalFuelQuantityLiters(),
                cost.currencyCode(),
                cost.totalFuelCost(),
                cost.tripDistanceKm(),
                cost.costPerKm(),
                cost.litersPer100Km(),
                cost.fuelIssueCount(),
                cost.unpricedIssueCount(),
                cost.distanceStatus(),
                cost.calculationStatus(),
                lines,
                cost.calculatedAt()
        );
    }

    private TripFuelCostLineResponse mapLine(TripFuelCostLine line) {
        return new TripFuelCostLineResponse(
                line.fuelIssueId(),
                line.voucherNumber(),
                line.issuedAt(),
                line.quantityLiters(),
                line.unitPrice(),
                line.lineCost(),
                line.pricingSource(),
                line.currencyCode(),
                line.stationId(),
                line.fuelType()
        );
    }

    public record TripFuelCostResponse(
            UUID tripId,
            UUID vehicleId,
            BigDecimal totalFuelQuantityLiters,
            String currencyCode,
            BigDecimal totalFuelCost,
            BigDecimal tripDistanceKm,
            BigDecimal costPerKm,
            BigDecimal litersPer100Km,
            int fuelIssueCount,
            int unpricedIssueCount,
            TripDistanceStatus distanceStatus,
            TripFuelCostCalculationStatus calculationStatus,
            List<TripFuelCostLineResponse> lines,
            OffsetDateTime calculatedAt
    ) {
    }

    public record TripFuelCostLineResponse(
            UUID fuelIssueId,
            String voucherNumber,
            OffsetDateTime issuedAt,
            BigDecimal quantityLiters,
            BigDecimal unitPrice,
            BigDecimal lineCost,
            PricingSource pricingSource,
            String currencyCode,
            UUID stationId,
            String fuelType
    ) {
    }
}