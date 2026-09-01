package com.transportlogistics.app.delivery;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
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
public class DeliveryBatchConcurrencyPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = com.transportlogistics.app.tenancy.CanonicalTenant.ID;

    @Autowired
    private DeliveryBatchUseCase batchUseCase;

    @Autowired
    private DeliveryBatchRepository batchRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private DeliveryRiderRepository riderRepository;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    @Autowired
    private DeliveryZoneLookupPort zoneLookupPort;

    @MockBean
    private DriverEligibilityPort driverEligibilityPort;

    @MockBean
    private com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort locationLookupPort;

    private final OffsetDateTime now = OffsetDateTime.now();

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
            return Optional.of(new com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort.LocationReference(id, "LOC-1", "Loc 1", "Addr", 15.0, 15.0, true));
        });
    }

    private DeliveryZone seedZone(String code) {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary boundary =
                new com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary(coords);
        return zoneRepository.save(new DeliveryZone(
                UUID.randomUUID(), TENANT_A, code, "Zone " + code, "Concurrency Zone",
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneType.URBAN_DENSE,
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus.ACTIVE,
                true, 100, null, boundary, 0, 0L, now, "admin", now, "admin"
        ));
    }

    private DeliveryOrder seedOrder(String prefix) {
        int r = (int) (Math.random() * 900000) + 100000;
        return orderRepository.save(new DeliveryOrder(
                new DeliveryId(UUID.randomUUID()),
                new DeliveryNumber("DEL-2026-" + r),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(2)),
                "Fragile package",
                DeliveryStatus.READY_FOR_ASSIGNMENT,
                0L,
                now,
                now,
                "admin",
                "admin"
        ));
    }

    @Test
    @DisplayName("Concurrency Case A: Simultaneous attempt to add the same DeliveryOrder to two distinct batches")
    void doubleBatching_raceCondition_onlyOneSucceeds() throws Exception {
        DeliveryZone zone = seedZone("BAT-CZ1-" + UUID.randomUUID().toString().substring(0, 6));
        DeliveryOrder order = seedOrder("DEL-2026-800001");

        DeliveryBatch batch1 = batchUseCase.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zone.id(), null, 5, List.of(), null
        ));
        DeliveryBatch batch2 = batchUseCase.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zone.id(), null, 5, List.of(), null
        ));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                batchUseCase.addOrdersToBatch(batch1.id(), new DeliveryBatchUseCase.AddOrdersToBatchCommand(List.of(order.id().value())));
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("Thread 1 error: " + e.getMessage());
                e.printStackTrace(System.out);
                failureCount.incrementAndGet();
            }
        }));

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                batchUseCase.addOrdersToBatch(batch2.id(), new DeliveryBatchUseCase.AddOrdersToBatchCommand(List.of(order.id().value())));
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("Thread 2 error: " + e.getMessage());
                e.printStackTrace(System.out);
                failureCount.incrementAndGet();
            }
        }));

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one should succeed, and one should fail due to active membership conflict
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Optional<DeliveryBatchOrder> activeMembership = batchRepository.findActiveMembershipByDeliveryOrderId(TENANT_A, order.id().value());
        assertThat(activeMembership).isPresent();
    }

    @Test
    @DisplayName("Concurrency Case B: Concurrent batch cancellation vs add order")
    void cancelVsAddOrder_raceCondition_safeOutcome() throws Exception {
        DeliveryZone zone = seedZone("BAT-CZ2-" + UUID.randomUUID().toString().substring(0, 6));
        DeliveryOrder order = seedOrder("DEL-2026-800002");

        DeliveryBatch batch = batchUseCase.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zone.id(), null, 5, List.of(), null
        ));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                batchUseCase.cancelBatch(batch.id());
            } catch (Exception ignored) {}
        }));

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                batchUseCase.addOrdersToBatch(batch.id(), new DeliveryBatchUseCase.AddOrdersToBatchCommand(List.of(order.id().value())));
            } catch (Exception ignored) {}
        }));

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        DeliveryBatch finalBatch = batchUseCase.getBatch(batch.id());
        if (finalBatch.status() == DeliveryBatchStatus.CANCELLED) {
            // No active memberships should remain if cancelled
            List<DeliveryBatchOrder> activeMembers = batchRepository.findActiveOrderMembershipsByBatchId(TENANT_A, batch.id());
            assertThat(activeMembers).isEmpty();
        }
    }
}
