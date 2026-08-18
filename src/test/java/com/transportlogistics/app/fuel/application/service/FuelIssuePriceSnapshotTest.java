package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.TripDistancePort;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.transportlogistics.app.fuel.application.ports.out.FuelActorPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import com.transportlogistics.app.fuel.application.ports.out.VehicleFuelContextPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.transportlogistics.app.fuel.application.service.FuelIssueService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.Clock;
import static org.mockito.Mockito.*;

import com.transportlogistics.app.fuel.application.ports.out.FuelLimitPolicyRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueHistoryRepository;

@ExtendWith(MockitoExtension.class)
class FuelIssuePriceSnapshotTest {

    @Mock
    private FuelIssueRepository issueRepo;
    @Mock
    private FuelIssueHistoryRepository historyRepo;
    @Mock
    private FuelStationRepository stationRepo;
    @Mock
    private FuelLimitPolicyRepository limits;
    @Mock
    private FuelPriceRepository priceRepo;
    @Mock
    private TripDistancePort distancePort;
    @Mock
    private TripFuelContextPort contextPort;
    @Mock
    private VehicleFuelContextPort vehicles;
    @Mock
    private FuelActorPort actors;
    // Test constants
    private final UUID driverId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID tripId = UUID.randomUUID();
    private final UUID stationId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();
    private FuelIssueService issueService;
    private TripFuelCostService costService;

    @BeforeEach
    void setUp() {
        when(issueRepo.save(any(FuelIssue.class))).thenAnswer(i -> i.getArgument(0));
        when(historyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        var actors = mock(FuelActorPort.class);
        var vouchers = mock(FuelVoucherGenerator.class);
        var transaction = mock(FuelTransaction.class);
        var events = mock(FuelEventPublisher.class);
        // Stub actor and vehicle lookup
        when(actors.find("tester")).thenReturn(Optional.of(new FuelActorPort.Actor(UUID.randomUUID(), "tester")));
        lenient().when(vouchers.next(anyInt())).thenReturn("FUEL-2026-000001");
        when(transaction.execute(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        when(vehicles.find(vehicleId)).thenReturn(Optional.of(new VehicleFuelContextPort.VehicleContext(vehicleId, "REG-001", true, "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO)));
        when(limits.findApplicable(any())).thenReturn(java.util.List.of());
        // Ensure issueRepo.findByIdForUpdate delegates to findById stubbed in each test
        when(issueRepo.findByIdForUpdate(any(UUID.class))).thenAnswer(i -> issueRepo.findById((java.util.UUID) i.getArgument(0)));
        issueService = new FuelIssueService(issueRepo, historyRepo, stationRepo, limits, vehicles, contextPort, actors, vouchers, transaction, events, priceRepo, Clock.fixed(now.toInstant(), java.time.ZoneOffset.UTC));
        costService = new TripFuelCostService(issueRepo, stationRepo, priceRepo, distancePort, contextPort);
        when(contextPort.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(
                tripId, "TRIP-001", "ASSIGNED", vehicleId, driverId, now.minusHours(5), now
        )));
    }

    @Test
    void historicalPriceIsSnapshotAtIssueTime() {
        FuelPrice initialPrice = new FuelPrice(UUID.randomUUID(), vendorId, "DIESEL", LocalDate.of(2026, 1, 1), null,
                new BigDecimal("300.00"), "LKR", true, now, now);
        when(priceRepo.findEffective(eq(vendorId), eq("DIESEL"), any(LocalDate.class))).thenReturn(Optional.of(initialPrice));
        when(stationRepo.findById(stationId)).thenReturn(Optional.of(new FuelStation(stationId, "ST-01", "Station", FuelStationType.EXTERNAL, true, vendorId, UUID.randomUUID())));

        FuelIssue draft = new FuelIssue(UUID.randomUUID(), "VOUCH", vehicleId, tripId, driverId,
                "DIESEL",
                new BigDecimal("10.000"), null, null, stationId, new BigDecimal("1000"), null, now, FuelIssueStatus.AUTHORIZED, null, null, null, null, now, now);
        when(issueRepo.findById(any())).thenReturn(Optional.of(draft));
        FuelIssue issued = issueService.issue(draft.id(), "tester");
        assertEquals(new BigDecimal("300.00"), issued.unitPrice());

        when(issueRepo.findByTripId(tripId)).thenReturn(java.util.List.of(issued));

        FuelPrice laterPrice = new FuelPrice(UUID.randomUUID(), vendorId, "DIESEL", LocalDate.of(2026, 1, 1), null,
                new BigDecimal("350.00"), "LKR", true, now, now);
        lenient().when(priceRepo.findEffective(eq(vendorId), eq("DIESEL"), any(LocalDate.class))).thenReturn(Optional.of(laterPrice));
        when(distancePort.getTripDistance(tripId)).thenReturn(new TripDistanceSummary(tripId, vehicleId, new BigDecimal("10000.000"), new BigDecimal("10100.000"), new BigDecimal("100.000"), TripDistanceStatus.CALCULATED));

        var result = costService.getTripFuelCost(tripId);
        assertEquals(new BigDecimal("3000.00"), result.totalFuelCost());
        assertEquals(TripFuelCostCalculationStatus.COMPLETE, result.calculationStatus());
    }

    @Test
    void explicitUnitPriceIsNotOverwrittenByCatalogue() {
        FuelIssue explicit = new FuelIssue(UUID.randomUUID(), "VOUCH", vehicleId, tripId, driverId,
                "DIESEL",
                new BigDecimal("5.000"), new BigDecimal("400.00"), new BigDecimal("2000.00"), stationId, new BigDecimal("500"), null, now, FuelIssueStatus.AUTHORIZED, null, null, null, null, now, now);
        when(issueRepo.findById(any())).thenReturn(Optional.of(explicit));
        lenient().when(priceRepo.findEffective(any(), any(), any())).thenReturn(Optional.of(new FuelPrice(UUID.randomUUID(), vendorId, "DIESEL", LocalDate.of(2026, 1, 1), null, new BigDecimal("350.00"), "LKR", true, now, now)));
        lenient().when(stationRepo.findById(stationId)).thenReturn(Optional.of(new FuelStation(stationId, "ST-01", "Station", FuelStationType.EXTERNAL, true, vendorId, UUID.randomUUID())));

        FuelIssue issued = issueService.issue(explicit.id(), "tester");
        assertEquals(new BigDecimal("400.00"), issued.unitPrice());
        verify(priceRepo, never()).findEffective(any(), any(), any());
    }
}
