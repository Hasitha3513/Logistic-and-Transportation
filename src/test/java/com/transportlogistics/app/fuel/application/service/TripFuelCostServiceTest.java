package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fuel.domain.model.PricingSource;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.TripDistancePort;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripFuelCostServiceTest {

    @Mock
    private FuelIssueRepository fuelIssues;
    @Mock
    private FuelStationRepository stations;
    @Mock
    private FuelPriceRepository fuelPrices;
    @Mock
    private TripDistancePort tripDistances;
    @Mock
    private TripFuelContextPort tripContexts;

    private TripFuelCostService service;

    private final UUID tripId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID stationId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new TripFuelCostService(fuelIssues, stations, fuelPrices, tripDistances, tripContexts);
    }

    private void mockTripContext() {
        when(tripContexts.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(
                tripId, "TRIP-001", "COMPLETED", vehicleId, UUID.randomUUID(), now.minusHours(5), now
        )));
    }

    private FuelIssue issue(UUID id, BigDecimal qty, BigDecimal unitPrice, FuelIssueStatus status) {
        return new FuelIssue(
                id, "VOUCHER-" + id.toString().substring(0, 4), vehicleId, tripId, UUID.randomUUID(),
                "DIESEL", qty, unitPrice, unitPrice != null ? qty.multiply(unitPrice) : null,
                stationId, new BigDecimal("1000"), null, now, status, UUID.randomUUID(),
                UUID.randomUUID(), now, null, now, now
        );
    }

    @Test
    void tripNotFoundThrowsException() {
        when(tripContexts.find(tripId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getTripFuelCost(tripId));
    }

    @Test
    void nullTripIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getTripFuelCost(null));
    }

    @Test
    void singlePricedFuelIssueWithValidDistanceCalculatesCompleteCost() {
        mockTripContext();
        var issue1 = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issue1));
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10200.000"),
                new BigDecimal("200.000"), TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.COMPLETE, result.calculationStatus());
        assertEquals(new BigDecimal("20.000"), result.totalFuelQuantityLiters());
        assertEquals(new BigDecimal("6000.00"), result.totalFuelCost());
        assertEquals(new BigDecimal("200.000"), result.tripDistanceKm());
        assertEquals(new BigDecimal("30.00"), result.costPerKm());
        assertEquals(new BigDecimal("10.00"), result.litersPer100Km());
        assertEquals(1, result.fuelIssueCount());
        assertEquals(0, result.unpricedIssueCount());
        assertEquals(1, result.lines().size());
        assertEquals(PricingSource.ISSUE_PRICE, result.lines().getFirst().pricingSource());
    }

    @Test
    void multiplePricedFuelIssuesSumQuantitiesAndCosts() {
        mockTripContext();
        var issue1 = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);
        var issue2 = issue(UUID.randomUUID(), new BigDecimal("10.000"), new BigDecimal("310.00"), FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issue1, issue2));
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10200.000"),
                new BigDecimal("200.000"), TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.COMPLETE, result.calculationStatus());
        assertEquals(new BigDecimal("30.000"), result.totalFuelQuantityLiters());
        assertEquals(new BigDecimal("9100.00"), result.totalFuelCost());
        assertEquals(new BigDecimal("200.000"), result.tripDistanceKm());
        assertEquals(new BigDecimal("45.50"), result.costPerKm());
        assertEquals(new BigDecimal("15.00"), result.litersPer100Km());
        assertEquals(2, result.fuelIssueCount());
    }

    @Test
    void nonIssuedStatusIssuesAreExcludedFromCalculation() {
        mockTripContext();
        var draft = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.DRAFT);
        var auth = issue(UUID.randomUUID(), new BigDecimal("15.000"), new BigDecimal("300.00"), FuelIssueStatus.AUTHORIZED);
        var cancelled = issue(UUID.randomUUID(), new BigDecimal("10.000"), new BigDecimal("300.00"), FuelIssueStatus.CANCELLED);
        var issued = issue(UUID.randomUUID(), new BigDecimal("5.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);

        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(draft, auth, cancelled, issued));
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10100.000"),
                new BigDecimal("100.000"), TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(1, result.fuelIssueCount());
        assertEquals(new BigDecimal("5.000"), result.totalFuelQuantityLiters());
        assertEquals(new BigDecimal("1500.00"), result.totalFuelCost());
    }

    @Test
    void missingHistoricalIssuePriceDoesNotFallbackToCatalogue() {
        mockTripContext();
        var issueNoPrice = issue(UUID.randomUUID(), new BigDecimal("25.000"), null, FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issueNoPrice));
        // No station or catalogue lookup should be performed
        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.PARTIAL, result.calculationStatus());
        assertEquals(PricingSource.UNPRICED, result.lines().getFirst().pricingSource());
        assertNull(result.lines().getFirst().unitPrice());
        assertNull(result.lines().getFirst().lineCost());
        assertEquals(1, result.unpricedIssueCount());
        assertEquals(1, result.fuelIssueCount());
        assertNull(result.costPerKm());
    }

    @Test
    void unresolvablePriceYieldsPartialCalculationStatus() {
        mockTripContext();
        var issue1 = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);
        var issueUnpriced = issue(UUID.randomUUID(), new BigDecimal("10.000"), null, FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issue1, issueUnpriced));

        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10200.000"),
                new BigDecimal("200.000"), TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.PARTIAL, result.calculationStatus());
        assertEquals(1, result.unpricedIssueCount());
        assertEquals(2, result.fuelIssueCount());
        assertEquals(new BigDecimal("30.000"), result.totalFuelQuantityLiters());
        assertEquals(new BigDecimal("6000.00"), result.totalFuelCost());
        assertNull(result.costPerKm());
    }

    @Test
    void incompleteTripDistanceYieldsPartialCalculationStatusAndNullCostPerKm() {
        mockTripContext();
        var issue1 = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issue1));
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), null,
                null, TripDistanceStatus.PENDING_END
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.PARTIAL, result.calculationStatus());
        assertNull(result.tripDistanceKm());
        assertNull(result.costPerKm());
        assertEquals(new BigDecimal("6000.00"), result.totalFuelCost());
    }

    @Test
    void zeroDistanceYieldsNullCostPerKmWithoutDivisionByZero() {
        mockTripContext();
        var issue1 = issue(UUID.randomUUID(), new BigDecimal("20.000"), new BigDecimal("300.00"), FuelIssueStatus.ISSUED);
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of(issue1));
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10000.000"),
                BigDecimal.ZERO, TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.COMPLETE, result.calculationStatus());
        assertEquals(BigDecimal.ZERO, result.tripDistanceKm());
        assertNull(result.costPerKm());
    }

    @Test
    void tripWithNoFuelIssuesHasZeroTotalsAndCompleteStatus() {
        mockTripContext();
        when(fuelIssues.findByTripId(tripId)).thenReturn(List.of());
        when(tripDistances.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(
                tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10100.000"),
                new BigDecimal("100.000"), TripDistanceStatus.CALCULATED
        ));

        var result = service.getTripFuelCost(tripId);

        assertEquals(TripFuelCostCalculationStatus.COMPLETE, result.calculationStatus());
        assertEquals(BigDecimal.ZERO.setScale(3), result.totalFuelQuantityLiters());
        assertEquals(BigDecimal.ZERO.setScale(2), result.totalFuelCost());
        assertEquals(0, result.fuelIssueCount());
        assertNull(result.costPerKm());
    }
}