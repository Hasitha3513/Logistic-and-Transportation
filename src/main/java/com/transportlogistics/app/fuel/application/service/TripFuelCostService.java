package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fuel.domain.model.PricingSource;
import com.transportlogistics.app.fuel.TripFuelCost;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import com.transportlogistics.app.fuel.TripFuelCostLine;
import com.transportlogistics.app.fuel.application.ports.in.TripFuelCostUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.TripDistancePort;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TripFuelCostService implements TripFuelCostUseCase {

    private static final String DEFAULT_CURRENCY = "LKR";
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_VOLUME = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

    private final FuelIssueRepository fuelIssues;
    private final FuelStationRepository stations;
    private final FuelPriceRepository fuelPrices;
    private final TripDistancePort tripDistances;
    private final TripFuelContextPort tripContexts;

    public TripFuelCostService(FuelIssueRepository fuelIssues,
                               FuelStationRepository stations,
                               FuelPriceRepository fuelPrices,
                               TripDistancePort tripDistances,
                               TripFuelContextPort tripContexts) {
        this.fuelIssues = fuelIssues;
        this.stations = stations;
        this.fuelPrices = fuelPrices;
        this.tripDistances = tripDistances;
        this.tripContexts = tripContexts;
    }

    @Override
    public TripFuelCost getTripFuelCost(UUID tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Trip id is required");
        }

        var tripContext = tripContexts.find(tripId).orElseThrow(() ->
                new NotFoundException("TRIP_NOT_FOUND", "Trip not found: " + tripId));

        var issuedRecords = fuelIssues.findByTripId(tripId).stream()
                .filter(issue -> issue.status() == FuelIssueStatus.ISSUED)
                .toList();

        List<TripFuelCostLine> lines = new ArrayList<>();
        for (FuelIssue issue : issuedRecords) {
            lines.add(resolveLine(issue));
        }

        BigDecimal totalQuantity = lines.stream()
                .map(TripFuelCostLine::quantityLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal totalCost = lines.stream()
                .map(TripFuelCostLine::lineCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int unpricedCount = (int) lines.stream().filter(l -> l.lineCost() == null).count();

        String currency = lines.stream()
                .map(TripFuelCostLine::currencyCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_CURRENCY);

        TripDistanceSummary distanceSummary = null;
        try {
            distanceSummary = tripDistances.getTripDistance(tripId);
        } catch (Exception ignored) {
            // Fleet distance lookup fallback
        }

        TripDistanceStatus distanceStatus = distanceSummary != null ? distanceSummary.status() : TripDistanceStatus.MISMATCH;
        BigDecimal distanceKm = (distanceSummary != null && distanceSummary.status() == TripDistanceStatus.CALCULATED)
                ? distanceSummary.distanceTravelledKm()
                : null;

        BigDecimal costPerKm = null;
        BigDecimal litersPer100Km = null;
        if (distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0 && unpricedCount == 0 && !lines.isEmpty()) {
            costPerKm = totalCost.divide(distanceKm, 2, RoundingMode.HALF_UP);
            litersPer100Km = totalQuantity.multiply(new BigDecimal("100")).divide(distanceKm, 2, RoundingMode.HALF_UP);
        }

        TripFuelCostCalculationStatus calcStatus;
        if (lines.isEmpty()) {
            calcStatus = TripFuelCostCalculationStatus.COMPLETE;
        } else if (unpricedCount > 0 || distanceStatus != TripDistanceStatus.CALCULATED) {
            calcStatus = TripFuelCostCalculationStatus.PARTIAL;
        } else {
            calcStatus = TripFuelCostCalculationStatus.COMPLETE;
        }

        return new TripFuelCost(
                tripId,
                tripContext.vehicleId(),
                totalQuantity,
                currency,
                totalCost,
                distanceKm,
                costPerKm,
                litersPer100Km,
                lines.size(),
                unpricedCount,
                distanceStatus,
                calcStatus,
                lines,
                OffsetDateTime.now()
        );
    }

    private TripFuelCostLine resolveLine(FuelIssue issue) {
        BigDecimal unitPrice = null;
        BigDecimal lineCost = null;
        PricingSource source = PricingSource.UNPRICED;
        String currency = DEFAULT_CURRENCY;

        if (issue.unitPrice() != null) {
            unitPrice = issue.unitPrice().setScale(2, RoundingMode.HALF_UP);
            lineCost = issue.quantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            source = PricingSource.ISSUE_PRICE;
    }

        return new TripFuelCostLine(
                issue.id(),
                issue.voucherNumber(),
                issue.issueDateTime() != null ? issue.issueDateTime() : issue.createdAt(),
                issue.quantity() != null ? issue.quantity().setScale(3, RoundingMode.HALF_UP) : ZERO_VOLUME,
                unitPrice,
                lineCost,
                source,
                currency,
                issue.stationId(),
                issue.fuelType()
        );
    }
}