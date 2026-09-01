package com.transportlogistics.app.delivery;

import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAssignmentStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DeliveryRiderConcurrencyPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = com.transportlogistics.app.tenancy.CanonicalTenant.ID;

    @Autowired
    private DeliveryRiderUseCase riderUseCase;

    @Autowired
    private DeliveryRiderRepository riderRepository;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    @MockBean
    private DriverEligibilityPort driverEligibilityPort;

    @MockBean
    private DeliveryLocationLookupPort locationLookupPort;

    private final OffsetDateTime now = OffsetDateTime.now();
    private UUID zoneAId;

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));
        when(tenantContext.currentTenantId()).thenReturn(Optional.of(TENANT_A));
        when(driverEligibilityPort.findDriver(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(new DriverEligibilityPort.DriverSummary(id, "EMP-" + id.toString().substring(0, 4), "John", "Doe", "AVAILABLE", true));
        });
        when(driverEligibilityPort.isOperationallyEligible(any(), any(), any())).thenReturn(true);
        when(locationLookupPort.findLocation(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(new DeliveryLocationLookupPort.LocationReference(id, "LOC-1", "Loc 1", "Addr", 10.5, 10.5, true));
        });

        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0), new DeliveryZoneCoordinate(10.0, 11.0),
                new DeliveryZoneCoordinate(11.0, 11.0), new DeliveryZoneCoordinate(10.0, 10.0)
        );
        com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary boundary =
                new com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary(coords);
        DeliveryZone zoneA = new DeliveryZone(
                UUID.randomUUID(), TENANT_A, "ZONE-CONC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Concurrency Zone", "Desc",
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneType.URBAN_DENSE,
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus.ACTIVE,
                true, 20, null, boundary, 1, 0L, now, "test", now, "test"
        );
        zoneAId = zoneRepository.save(zoneA).id();
    }

    @Test
    @DisplayName("Concurrency Matrix Case A: Simultaneous assignment race on same delivery order -> exactly 1 active assignment")
    void concurrentAssignmentOnSameOrderRace() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID driver1 = UUID.randomUUID();
        UUID driver2 = UUID.randomUUID();

        UUID rider1Id = riderUseCase.onboardRider(
                new DeliveryRiderUseCase.OnboardRiderCommand("RDR-RACE-1-" + UUID.randomUUID().toString().substring(0, 5), driver1, DeliveryRiderType.FULL_TIME, DeliveryTransportMode.MOTORBIKE, zoneAId, Set.of(), 5),
                "admin"
        ).getId();

        UUID rider2Id = riderUseCase.onboardRider(
                new DeliveryRiderUseCase.OnboardRiderCommand("RDR-RACE-2-" + UUID.randomUUID().toString().substring(0, 5), driver2, DeliveryRiderType.FULL_TIME, DeliveryTransportMode.BICYCLE, zoneAId, Set.of(), 5),
                "admin"
        ).getId();

        int r1 = (int) (Math.random() * 900000) + 100000;
        DeliveryOrder order = new DeliveryOrder(
                new DeliveryId(orderId), new DeliveryNumber("DEL-2026-" + r1), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(2)), "Leave at door", DeliveryStatus.READY_FOR_ASSIGNMENT,
                0L, now, now, "system", "system"
        );
        orderRepository.save(order);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<UUID> riders = List.of(rider1Id, rider2Id);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final UUID targetRider = riders.get(i);
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    riderUseCase.assignRider(orderId, new DeliveryRiderUseCase.AssignRiderCommand(targetRider, false, null), "dispatcher");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        int activeAssignments = riderRepository.findAssignmentHistoryForOrder(orderId, TENANT_A).stream()
                .filter(a -> a.getStatus() == DeliveryRiderAssignmentStatus.ACTIVE)
                .toList().size();
        assertThat(activeAssignments).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrency Matrix Case B: Concurrent assignment race exceeding rider max concurrent capacity")
    void concurrentAssignmentExceedingRiderCapacity() throws Exception {
        UUID driverId = UUID.randomUUID();
        // Capacity of 1
        UUID riderId = riderUseCase.onboardRider(
                new DeliveryRiderUseCase.OnboardRiderCommand("RDR-CAP-" + UUID.randomUUID().toString().substring(0, 5), driverId, DeliveryRiderType.FULL_TIME, DeliveryTransportMode.VAN, zoneAId, Set.of(), 1),
                "admin"
        ).getId();

        UUID order1Id = UUID.randomUUID();
        UUID order2Id = UUID.randomUUID();

        int r2 = (int) (Math.random() * 900000) + 100000;
        int r3 = (int) (Math.random() * 900000) + 100000;
        DeliveryOrder o1 = new DeliveryOrder(
                new DeliveryId(order1Id), new DeliveryNumber("DEL-2026-" + r2), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(2)), null, DeliveryStatus.READY_FOR_ASSIGNMENT,
                0L, now, now, "system", "system"
        );
        DeliveryOrder o2 = new DeliveryOrder(
                new DeliveryId(order2Id), new DeliveryNumber("DEL-2026-" + r3), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(2)), null, DeliveryStatus.READY_FOR_ASSIGNMENT,
                0L, now, now, "system", "system"
        );
        orderRepository.save(o1);
        orderRepository.save(o2);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        List<UUID> orders = List.of(order1Id, order2Id);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final UUID targetOrder = orders.get(i);
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    riderUseCase.assignRider(targetOrder, new DeliveryRiderUseCase.AssignRiderCommand(riderId, false, null), "dispatcher");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    capacityExceededCount.incrementAndGet();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(capacityExceededCount.get()).isEqualTo(1);

        int activeAssignments = riderRepository.countActiveAssignmentsForRider(riderId, TENANT_A);
        assertThat(activeAssignments).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrency Matrix Case D: Concurrent overlapping shift creation race")
    void concurrentOverlappingShiftCreationRace() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID riderId = riderUseCase.onboardRider(
                new DeliveryRiderUseCase.OnboardRiderCommand("RDR-SHIFT-" + UUID.randomUUID().toString().substring(0, 5), driverId, DeliveryRiderType.FULL_TIME, DeliveryTransportMode.MOTORBIKE, zoneAId, Set.of(), 5),
                "admin"
        ).getId();

        LocalDate shiftDate = LocalDate.of(2026, 9, 2);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger overlapFailCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    riderUseCase.createShift(
                            riderId,
                            new DeliveryRiderUseCase.CreateShiftCommand(shiftDate, LocalTime.of(9, 0), LocalTime.of(17, 0), null, 5),
                            "admin"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    overlapFailCount.incrementAndGet();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(overlapFailCount.get()).isEqualTo(1);

        List<DeliveryRiderShift> shifts = riderRepository.findActiveShiftsByRiderIdAndDate(riderId, shiftDate, TENANT_A);
        assertThat(shifts).hasSize(1);
    }
}
