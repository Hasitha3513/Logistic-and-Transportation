package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

class FuelIssueServiceTest {
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID stationId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private FuelIssueRepository issues;
    private FuelIssueHistoryRepository history;
    private FuelStationRepository stations;
    private FuelLimitPolicyRepository limits;
    private VehicleFuelContextPort vehicles;
    private TripFuelContextPort trips;
    private FuelEventPublisher events;
    private FuelPriceRepository priceRepo;
    private BunkerTankRepository bunkerTanks;
    private BunkerStockLedgerRepository bunkerMovements;
    private FuelIssueService service;
    private BunkerTank activeTank;

    @BeforeEach
    void setUp() {
        issues = mock(FuelIssueRepository.class);
        history = mock(FuelIssueHistoryRepository.class);
        stations = mock(FuelStationRepository.class);
        limits = mock(FuelLimitPolicyRepository.class);
        vehicles = mock(VehicleFuelContextPort.class);
        trips = mock(TripFuelContextPort.class);
        events = mock(FuelEventPublisher.class);
        bunkerTanks = mock(BunkerTankRepository.class);
        bunkerMovements = mock(BunkerStockLedgerRepository.class);
        var actors = mock(FuelActorPort.class);
        var vouchers = mock(FuelVoucherGenerator.class);
        var transaction = mock(FuelTransaction.class);
        when(transaction.execute(any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
        when(actors.find("operator")).thenReturn(Optional.of(new FuelActorPort.Actor(actorId, "operator")));
        when(vouchers.next(2026)).thenReturn("FUEL-2026-000001");
        when(issues.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stations.findById(stationId)).thenReturn(Optional.of(new FuelStation(stationId, "MAIN", "Main Depot",
                FuelStationType.INTERNAL, true, null, null)));
        when(vehicles.find(vehicleId)).thenReturn(Optional.of(new VehicleFuelContextPort.VehicleContext(vehicleId,
                "WP-1000", true, "AVAILABLE", new BigDecimal("900"), new BigDecimal("100"))));
        when(limits.findApplicable(vehicleId)).thenReturn(List.of());
        priceRepo = mock(FuelPriceRepository.class);
        when(priceRepo.findEffective(any(), any(), any())).thenReturn(Optional.empty());

        activeTank = new BunkerTank(UUID.randomUUID(), stationId, "BNK-01", "Main Tank", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(), true, OffsetDateTime.now(), OffsetDateTime.now());
        when(bunkerTanks.findActiveByStationAndFuelType(stationId, "DIESEL")).thenReturn(Optional.of(activeTank));
        when(bunkerTanks.findActiveByStationAndFuelTypeForUpdate(stationId, "DIESEL")).thenReturn(Optional.of(activeTank));
        when(bunkerTanks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bunkerMovements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new FuelIssueService(issues, history, stations, limits, vehicles, trips, actors, vouchers,
                transaction, events, priceRepo, null, bunkerTanks, bunkerMovements,
                Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsValidFuelIssueWithVoucherAndHistory() {
        var created = service.create(command(null, vehicleId, new BigDecimal("50")), "operator");
        assertEquals("FUEL-2026-000001", created.voucherNumber());
        assertEquals(FuelIssueStatus.DRAFT, created.status());
        assertEquals(actorId, created.requestedBy());
        verify(history).save(argThat(entry -> entry.action().equals("CREATED")));
    }

    @Test
    void rejectsNonexistentAndInactiveVehicle() {
        when(vehicles.find(vehicleId)).thenReturn(Optional.empty());
        assertCode("FUEL_VEHICLE_NOT_FOUND", () -> service.create(command(null, vehicleId, new BigDecimal("10")), "operator"));
        when(vehicles.find(vehicleId)).thenReturn(Optional.of(new VehicleFuelContextPort.VehicleContext(vehicleId,
                "WP-1000", false, "RETIRED", null, null)));
        assertCode("FUEL_VEHICLE_INELIGIBLE", () -> service.create(command(null, vehicleId, new BigDecimal("10")), "operator"));
    }

    @Test
    void validatesTripExistenceVehicleAndDriverCoherently() {
        var tripId = UUID.randomUUID();
        var driverId = UUID.randomUUID();
        when(trips.find(tripId)).thenReturn(Optional.empty());
        assertCode("FUEL_TRIP_NOT_FOUND", () -> service.create(command(tripId, vehicleId, new BigDecimal("10")), "operator"));

        when(trips.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(tripId, "TRIP-1",
                "ASSIGNED", UUID.randomUUID(), driverId, null, null)));
        assertCode("FUEL_VEHICLE_TRIP_MISMATCH", () -> service.create(command(tripId, vehicleId, new BigDecimal("10")), "operator"));

        when(trips.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(tripId, "TRIP-1",
                "ASSIGNED", vehicleId, driverId, null, null)));
        assertDoesNotThrow(() -> service.create(command(tripId, vehicleId, new BigDecimal("10")), "operator"));
    }

    @Test
    void rejectsIneligibleTripAndInactiveStation() {
        var tripId = UUID.randomUUID();
        when(trips.find(tripId)).thenReturn(Optional.of(new TripFuelContextPort.TripContext(tripId, "TRIP-1",
                "CLOSED", vehicleId, null, null, null)));
        assertCode("FUEL_TRIP_NOT_ELIGIBLE", () -> service.create(command(tripId, vehicleId, new BigDecimal("10")), "operator"));
        when(stations.findById(stationId)).thenReturn(Optional.of(new FuelStation(stationId, "OLD", "Old Station",
                FuelStationType.EXTERNAL, false, null, null)));
        assertCode("FUEL_STATION_INACTIVE", () -> service.create(command(null, vehicleId, new BigDecimal("10")), "operator"));
    }

    @Test
    void enforcesConfiguredLimitAndLatestReadings() {
        when(limits.findApplicable(vehicleId)).thenReturn(List.of(new FuelLimitPolicy(UUID.randomUUID(), vehicleId,
                new BigDecimal("25"), true)));
        assertCode("FUEL_LIMIT_EXCEEDED", () -> service.create(command(null, vehicleId, new BigDecimal("30")), "operator"));
        var lowReading = new FuelIssueUseCase.CreateCommand(vehicleId, null, null, "DIESEL", new BigDecimal("10"),
                null, stationId, new BigDecimal("899"), new BigDecimal("100"), time(), null);
        assertCode("INVALID_FUEL_ODOMETER", () -> service.create(lowReading, "operator"));
    }

    @Test
    void followsSubmitAuthorizeIssueLifecycleAndPublishesEvents() {
        var draft = issue(FuelIssueStatus.DRAFT);
        when(issues.findByIdForUpdate(draft.id())).thenReturn(Optional.of(draft),
                Optional.of(withStatus(draft, FuelIssueStatus.PENDING_AUTHORIZATION)),
                Optional.of(withStatus(draft, FuelIssueStatus.AUTHORIZED)),
                Optional.of(withStatus(draft, FuelIssueStatus.ISSUED)));

        assertEquals(FuelIssueStatus.PENDING_AUTHORIZATION, service.submit(draft.id(), "operator").status());
        assertEquals(FuelIssueStatus.AUTHORIZED, service.authorize(draft.id(), "approved", "operator").status());
        assertEquals(FuelIssueStatus.ISSUED, service.issue(draft.id(), "operator").status());
        assertThrows(ConflictException.class, () -> service.issue(draft.id(), "operator"));
        verify(events, times(2)).publish(any());
        verify(history, times(3)).save(any());
    }

    @Test
    void issueBeforeAuthorizationAndIssuedEditAreRejected() {
        var draft = issue(FuelIssueStatus.DRAFT);
        when(issues.findByIdForUpdate(draft.id())).thenReturn(Optional.of(draft));
        assertThrows(ConflictException.class, () -> service.issue(draft.id(), "operator"));
        var issued = withStatus(draft, FuelIssueStatus.ISSUED);
        when(issues.findByIdForUpdate(issued.id())).thenReturn(Optional.of(issued));
        assertThrows(ConflictException.class, () -> service.update(issued.id(), updateCommand(), "operator"));
    }

    @Test
    void cancellationAfterSubmissionRequiresReasonAndIsAudited() {
        var pending = issue(FuelIssueStatus.PENDING_AUTHORIZATION);
        when(issues.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending), Optional.of(pending));
        assertCode("FUEL_CANCELLATION_REASON_REQUIRED", () -> service.cancel(pending.id(), " ", "operator"));
        assertEquals(FuelIssueStatus.CANCELLED, service.cancel(pending.id(), "Trip cancelled", "operator").status());
        verify(events).publish(any());
    }

    private FuelIssueUseCase.CreateCommand command(UUID tripId, UUID vehicle, BigDecimal quantity) {
        return new FuelIssueUseCase.CreateCommand(vehicle, tripId, null, "diesel", quantity, new BigDecimal("2.50"),
                stationId, new BigDecimal("1000"), new BigDecimal("110"), time(), "Operational issue");
    }

    private FuelIssueUseCase.UpdateCommand updateCommand() {
        var c = command(null, vehicleId, new BigDecimal("10"));
        return new FuelIssueUseCase.UpdateCommand(c.vehicleId(), c.tripId(), c.driverId(), c.fuelType(), c.quantity(),
                c.unitPrice(), c.stationId(), c.odometer(), c.engineHours(), c.issueDateTime(), c.notes());
    }

    private FuelIssue issue(FuelIssueStatus status) {
        return new FuelIssue(UUID.randomUUID(), "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("10"), null, null, stationId, new BigDecimal("1000"), new BigDecimal("110"),
                time(), status, actorId, null, null, null, time(), time());
    }

    private FuelIssue withStatus(FuelIssue issue, FuelIssueStatus status) {
        return new FuelIssue(issue.id(), issue.voucherNumber(), issue.vehicleId(), issue.tripId(), issue.driverId(),
                issue.fuelType(), issue.quantity(), issue.unitPrice(), issue.totalAmount(), issue.stationId(),
                issue.odometer(), issue.engineHours(), issue.issueDateTime(), status, issue.requestedBy(),
                status == FuelIssueStatus.AUTHORIZED || status == FuelIssueStatus.ISSUED ? actorId : null,
                status == FuelIssueStatus.AUTHORIZED || status == FuelIssueStatus.ISSUED ? time() : null,
                issue.notes(), issue.createdAt(), issue.updatedAt());
    }

    private OffsetDateTime time() {
        return OffsetDateTime.parse("2026-08-15T00:00:00Z");
    }

    private void assertCode(String expected, Runnable action) {
        var error = assertThrows(BusinessRuleException.class, action::run);
        assertEquals(expected, error.code());
    }

    @Test
    void issueRecordsAuthoritativeVehicleReading() {
        var readings = mock(FuelVehicleReadingPort.class);
        var actors = mock(FuelActorPort.class);
        // Set authenticated operator using TestingAuthenticationToken
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("operator", "", List.of()));
        when(actors.find("operator")).thenReturn(Optional.of(new FuelActorPort.Actor(actorId, "operator")));
        var vouchers = mock(FuelVoucherGenerator.class);
        var transaction = mock(FuelTransaction.class);
        when(transaction.execute(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        var events = mock(FuelEventPublisher.class);

        var fuelService = new FuelIssueService(issues, history, stations, limits, vehicles, trips, actors, vouchers,
                transaction, events, priceRepo, readings, Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC));

        var issueId = UUID.randomUUID();
        var authorized = withStatus(new FuelIssue(issueId, "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("50"), null, null, stationId, new BigDecimal("10500"), new BigDecimal("150"),
                time(), FuelIssueStatus.AUTHORIZED, actorId, actorId, time(), null, time(), time()), FuelIssueStatus.AUTHORIZED);
        when(issues.findByIdForUpdate(issueId)).thenReturn(Optional.of(authorized));

        var issued = fuelService.issue(issueId, "operator");
        assertEquals(FuelIssueStatus.ISSUED, issued.status());

        verify(readings).record(eq(vehicleId), eq(issueId), eq(new BigDecimal("10500")), eq(new BigDecimal("150")),
                eq(time()), eq(actorId));
    }

    @Test
    void readingRejectionPreventsFuelIssuance() {
        var readings = mock(FuelVehicleReadingPort.class);
        var actors = mock(FuelActorPort.class);
        var vouchers = mock(FuelVoucherGenerator.class);
        var transaction = mock(FuelTransaction.class);
        when(transaction.execute(any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
        when(actors.find("operator")).thenReturn(Optional.of(new FuelActorPort.Actor(actorId, "operator")));
        when(issues.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new ConflictException("VEHICLE_READING_DECREASE", "Decreasing odometer"))
                .when(readings).record(any(), any(), any(), any(), any(), any());

        var priceRepo = mock(FuelPriceRepository.class);
        when(priceRepo.findEffective(any(), any(), any())).thenReturn(Optional.empty());
        var fuelService = new FuelIssueService(issues, history, stations, limits, vehicles, trips, actors, vouchers,
                transaction, events, priceRepo, readings, Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC));

        var issueId = UUID.randomUUID();
        var authorized = withStatus(new FuelIssue(issueId, "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("50"), null, null, stationId, new BigDecimal("9000"), null,
                time(), FuelIssueStatus.AUTHORIZED, actorId, actorId, time(), null, time(), time()), FuelIssueStatus.AUTHORIZED);
        when(issues.findByIdForUpdate(issueId)).thenReturn(Optional.of(authorized));

        assertThrows(ConflictException.class, () -> fuelService.issue(issueId, "operator"));
    }

    @Test
    void rejectsInternalIssueWhenNoActiveBunkerTank() {
        when(bunkerTanks.findActiveByStationAndFuelType(stationId, "DIESEL")).thenReturn(Optional.empty());
        assertCode("NO_ACTIVE_BUNKER_TANK", () -> service.create(command(null, vehicleId, new BigDecimal("50")), "operator"));
    }

    @Test
    void rejectsInternalIssueWhenBunkerStockIsInsufficient() {
        var lowTank = activeTank.withStock(new BigDecimal("20.000"));
        when(bunkerTanks.findActiveByStationAndFuelType(stationId, "DIESEL")).thenReturn(Optional.of(lowTank));
        assertCode("INSUFFICIENT_BUNKER_STOCK", () -> service.create(command(null, vehicleId, new BigDecimal("50")), "operator"));
    }

    @Test
    void deductsBunkerStockAndAppendsMovementOnIssue() {
        var issueId = UUID.randomUUID();
        var authorized = withStatus(new FuelIssue(issueId, "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("50"), null, null, stationId, new BigDecimal("10500"), new BigDecimal("150"),
                time(), FuelIssueStatus.AUTHORIZED, actorId, actorId, time(), null, time(), time()), FuelIssueStatus.AUTHORIZED);
        when(issues.findByIdForUpdate(issueId)).thenReturn(Optional.of(authorized));

        var issued = service.issue(issueId, "operator");
        assertEquals(FuelIssueStatus.ISSUED, issued.status());

        // Verify bunker tank stock decremented by 50 L (5000 - 50 = 4950)
        verify(bunkerTanks).save(argThat(tank -> tank.currentStockLiters().compareTo(new BigDecimal("4950.000")) == 0));
        // Verify ledger movement recorded
        verify(bunkerMovements).save(argThat(m -> m.movementType() == BunkerMovementType.FUEL_ISSUE
                && m.referenceType() == BunkerReferenceType.FUEL_ISSUE
                && m.referenceId().equals(issueId)
                && m.quantityLiters().compareTo(new BigDecimal("50")) == 0
                && m.resultingBalanceLiters().compareTo(new BigDecimal("4950.000")) == 0));
    }

    @Test
    void bypassesBunkerDeductionForExternalStation() {
        var extStationId = UUID.randomUUID();
        when(stations.findById(extStationId)).thenReturn(Optional.of(new FuelStation(extStationId, "EXT-01", "External Station",
                FuelStationType.EXTERNAL, true, UUID.randomUUID(), null)));

        var issueId = UUID.randomUUID();
        var authorized = withStatus(new FuelIssue(issueId, "FUEL-2026-000002", vehicleId, null, null, "DIESEL",
                new BigDecimal("50"), null, null, extStationId, new BigDecimal("10500"), new BigDecimal("150"),
                time(), FuelIssueStatus.AUTHORIZED, actorId, actorId, time(), null, time(), time()), FuelIssueStatus.AUTHORIZED);
        when(issues.findByIdForUpdate(issueId)).thenReturn(Optional.of(authorized));

        var issued = service.issue(issueId, "operator");
        assertEquals(FuelIssueStatus.ISSUED, issued.status());

        verify(bunkerTanks, never()).findActiveByStationAndFuelTypeForUpdate(eq(extStationId), any());
    }
}
