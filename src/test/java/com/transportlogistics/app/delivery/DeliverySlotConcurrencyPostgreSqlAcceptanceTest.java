package com.transportlogistics.app.delivery;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliverySlotUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DeliverySlotConcurrencyPostgreSqlAcceptanceTest {

    @Autowired
    private DeliverySlotUseCase slotUseCase;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    @MockBean
    private DeliveryLocationLookupPort locationLookupPort;

    private final UUID tenantId = com.transportlogistics.app.tenancy.CanonicalTenant.ID;
    private final LocalDate slotDate = LocalDate.now().plusDays(2);
    private DeliveryZone zone;

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "UTC")));
        when(tenantContext.currentTenantId()).thenReturn(Optional.of(tenantId));

        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8, 6.9),
                new DeliveryZoneCoordinate(79.9, 6.9),
                new DeliveryZoneCoordinate(79.9, 7.0),
                new DeliveryZoneCoordinate(79.8, 7.0),
                new DeliveryZoneCoordinate(79.8, 6.9)
        );
        DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(coords);
        zone = new DeliveryZone(
                UUID.randomUUID(), tenantId, "ZONE-CONCURRENCY-" + UUID.randomUUID().toString().substring(0, 5),
                "Concurrency Test Zone", "Desc", DeliveryZoneType.URBAN_DENSE,
                DeliveryZoneStatus.ACTIVE, true, 50, null, boundary, 1, 0L,
                OffsetDateTime.now(), "admin", OffsetDateTime.now(), "admin"
        );
        zoneRepository.save(zone);
    }

    @Test
    void shouldPreventOverbookingUnderConcurrentBookingsForLastCapacity() throws Exception {
        int capacity = 5;
        DeliverySlotUseCase.CreateSlotCommand command = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(10, 0), LocalTime.of(13, 0),
                DeliverySlotType.STANDARD, capacity, null, 0
        );
        DeliverySlot slot = slotUseCase.createSlot(command, "admin");

        UUID locId = UUID.randomUUID();
        when(locationLookupPort.findLocation(locId))
                .thenReturn(Optional.of(new DeliveryLocationLookupPort.LocationReference(locId, "LOC-1", "Dest Loc", "Address", 6.95, 79.85, true)));

        int totalThreads = 10;
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            String num = "DEL-2026-" + String.format("%06d", (int) (Math.random() * 900000 + 100000));
            DeliveryOrder order = DeliveryOrder.create(
                    new DeliveryId(UUID.randomUUID()), new DeliveryNumber(num), UUID.randomUUID(),
                    UUID.randomUUID(), locId, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                    new DeliveryWindow(OffsetDateTime.now(), OffsetDateTime.now().plusHours(4)),
                    "Fragile", OffsetDateTime.now(), "admin"
            ).markReadyForAssignment(OffsetDateTime.now(), "admin");
            orderRepository.save(order);
            orders.add(order);
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            DeliveryOrder o = orders.get(i);
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    slotUseCase.assignDeliveryOrder(
                            slot.getId(), new DeliverySlotUseCase.AssignSlotCommand(o.id().value(), false, null), "admin"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Exactly 'capacity' assignments should succeed or fail without exceeding max capacity
        DeliverySlot updatedSlot = slotUseCase.getSlot(slot.getId());
        assertEquals(capacity, successCount.get(), "Successful bookings must equal slot capacity");
        assertEquals(totalThreads - capacity, failureCount.get(), "Remaining concurrent requests must fail");
        assertEquals(capacity, updatedSlot.getReservedCapacity(), "Reserved capacity must not exceed slot max capacity");
    }

    @Test
    void shouldPreventDoubleActiveReservationForSameOrderAcrossSlots() throws Exception {
        DeliverySlotUseCase.CreateSlotCommand cmd1 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(8, 0), LocalTime.of(10, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );
        DeliverySlot slot1 = slotUseCase.createSlot(cmd1, "admin");

        DeliverySlotUseCase.CreateSlotCommand cmd2 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(14, 0), LocalTime.of(16, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );
        DeliverySlot slot2 = slotUseCase.createSlot(cmd2, "admin");

        UUID locId = UUID.randomUUID();
        when(locationLookupPort.findLocation(locId))
                .thenReturn(Optional.of(new DeliveryLocationLookupPort.LocationReference(locId, "LOC-2", "Dest Loc", "Address", 6.95, 79.85, true)));

        String num = "DEL-2026-" + String.format("%06d", (int) (Math.random() * 900000 + 100000));
        DeliveryOrder order = DeliveryOrder.create(
                new DeliveryId(UUID.randomUUID()), new DeliveryNumber(num), UUID.randomUUID(),
                UUID.randomUUID(), locId, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(OffsetDateTime.now(), OffsetDateTime.now().plusHours(4)),
                "Double Book Test", OffsetDateTime.now(), "admin"
        ).markReadyForAssignment(OffsetDateTime.now(), "admin");
        orderRepository.save(order);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        Future<?> f1 = executor.submit(() -> {
            try {
                latch.await();
                slotUseCase.assignDeliveryOrder(slot1.getId(), new DeliverySlotUseCase.AssignSlotCommand(order.id().value(), false, null), "admin");
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                latch.await();
                slotUseCase.assignDeliveryOrder(slot2.getId(), new DeliverySlotUseCase.AssignSlotCommand(order.id().value(), false, null), "admin");
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        latch.countDown();
        f1.get();
        f2.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "At most one active reservation can succeed for the same delivery order");
    }

    @Test
    void shouldPreventOverlappingSlotCreationRace() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        DeliverySlotUseCase.CreateSlotCommand cmd1 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                DeliverySlotType.EXPRESS, 10, null, 0
        );

        DeliverySlotUseCase.CreateSlotCommand cmd2 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate.plusDays(1), LocalTime.of(11, 0), LocalTime.of(13, 0),
                DeliverySlotType.EXPRESS, 10, null, 0
        );

        Future<?> f1 = executor.submit(() -> {
            try {
                latch.await();
                slotUseCase.createSlot(cmd1, "admin");
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                latch.await();
                slotUseCase.createSlot(cmd2, "admin");
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        latch.countDown();
        f1.get();
        f2.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "At most one overlapping slot creation can succeed");
    }
}
