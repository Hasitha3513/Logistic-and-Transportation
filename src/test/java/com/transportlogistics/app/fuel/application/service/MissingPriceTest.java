package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.TripDistancePort;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.Clock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissingPriceTest {

    @Mock
    private FuelIssueRepository issueRepo;
    @Mock
    private FuelStationRepository stationRepo;
    @Mock
    private FuelPriceRepository priceRepo;
    @Mock
    private TripDistancePort distancePort;
    @Mock
    private TripFuelContextPort contextPort;

    private FuelIssueService issueService;
    private TripFuelCostService costService;

    private final UUID tripId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    @BeforeEach
    void setUp() {
        
        issueService = new FuelIssueService(issueRepo, null, null, null, null, null, null, null, null, null, priceRepo, null, Clock.fixed(now.toInstant(), java.time.ZoneOffset.UTC));
        costService = new TripFuelCostService(issueRepo, stationRepo, priceRepo, distancePort, contextPort);
        when(contextPort.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(
                tripId, "TRIP-001", "COMPLETED", vehicleId, UUID.randomUUID(), now.minusHours(5), now
        )));
    }

    @Test
    void missingPriceResultsInPartialStatus() {
        // Issue without unit price and missing station info
        FuelIssue issue = new FuelIssue(UUID.randomUUID(), "VOUCH", vehicleId, tripId, UUID.randomUUID(), "DIESEL",
                new BigDecimal("10.000"), null, null, null, new BigDecimal("1000"), null, now,
                FuelIssueStatus.ISSUED, null, null, null, null, now, now);
        when(issueRepo.findByTripId(tripId)).thenReturn(java.util.List.of(issue));
        when(distancePort.getTripDistance(tripId)).thenReturn(new com.transportlogistics.app.fleet.TripDistanceSummary(tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10100.000"), new BigDecimal("100.000"), TripDistanceStatus.CALCULATED));
        var result = costService.getTripFuelCost(tripId);
        assertEquals(TripFuelCostCalculationStatus.PARTIAL, result.calculationStatus());
        assertEquals(1, result.unpricedIssueCount());
        assertEquals(new BigDecimal("10.000"), result.totalFuelQuantityLiters());
        assertEquals(new BigDecimal("0.00"), result.totalFuelCost());
    }
}
