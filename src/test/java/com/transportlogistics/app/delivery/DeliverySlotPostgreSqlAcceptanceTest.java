package com.transportlogistics.app.delivery;

import com.transportlogistics.app.delivery.adapters.outbound.persistence.DeliverySlotJpaRepository;
import com.transportlogistics.app.delivery.adapters.outbound.persistence.DeliverySlotReservationJpaRepository;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliverySlotUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DeliverySlotPostgreSqlAcceptanceTest {

    @Autowired
    private DeliverySlotUseCase slotUseCase;

    @Autowired
    private DeliverySlotJpaRepository slotJpaRepository;

    @Autowired
    private DeliverySlotReservationJpaRepository reservationJpaRepository;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    @MockBean
    private DeliveryLocationLookupPort locationLookupPort;

    private final UUID tenantId = com.transportlogistics.app.tenancy.CanonicalTenant.ID;
    private final LocalDate slotDate = LocalDate.now().plusDays(1);
    private DeliveryZone zone;

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "UTC")));
        when(tenantContext.currentTenantId()).thenReturn(Optional.of(tenantId));

        // Create test active zone
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8, 6.9),
                new DeliveryZoneCoordinate(79.9, 6.9),
                new DeliveryZoneCoordinate(79.9, 7.0),
                new DeliveryZoneCoordinate(79.8, 7.0),
                new DeliveryZoneCoordinate(79.8, 6.9)
        );
        DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(coords);
        zone = new DeliveryZone(
                UUID.randomUUID(), tenantId, "ZONE-SLOT-TEST-" + UUID.randomUUID().toString().substring(0, 5),
                "Slot Test Zone", "Desc", DeliveryZoneType.URBAN_DENSE,
                DeliveryZoneStatus.ACTIVE, true, 20, null, boundary, 1, 0L,
                OffsetDateTime.now(), "admin", OffsetDateTime.now(), "admin"
        );
        zoneRepository.save(zone);
    }

    @Test
    void shouldCreateAndRetrieveSlotOnPostgreSQL() {
        DeliverySlotUseCase.CreateSlotCommand command = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, OffsetDateTime.now().plusDays(1), 15
        );

        DeliverySlot created = slotUseCase.createSlot(command, "admin");
        assertNotNull(created.getId());
        assertEquals(10, created.getMaxCapacity());
        assertEquals(0, created.getReservedCapacity());
        assertEquals(10, created.getRemainingCapacity());

        DeliverySlot retrieved = slotUseCase.getSlot(created.getId());
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(zone.id(), retrieved.getDeliveryZoneId());
    }

    @Test
    void shouldPreventOverlappingSlotCreationOnSameZoneAndDate() {
        DeliverySlotUseCase.CreateSlotCommand command1 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(10, 0), LocalTime.of(13, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );
        slotUseCase.createSlot(command1, "admin");

        DeliverySlotUseCase.CreateSlotCommand overlapping = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(12, 0), LocalTime.of(15, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );

        assertThrows(ConflictException.class, () -> slotUseCase.createSlot(overlapping, "admin"));
    }

    @Test
    void shouldAllowAdjacentNonOverlappingSlots() {
        DeliverySlotUseCase.CreateSlotCommand command1 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );
        DeliverySlot slot1 = slotUseCase.createSlot(command1, "admin");

        DeliverySlotUseCase.CreateSlotCommand command2 = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(12, 0), LocalTime.of(15, 0),
                DeliverySlotType.STANDARD, 5, null, 0
        );
        DeliverySlot slot2 = slotUseCase.createSlot(command2, "admin");

        assertNotNull(slot1);
        assertNotNull(slot2);
    }

    @Test
    void shouldEnforceCapacityAndPreventOverbookingWithoutOverride() {
        DeliverySlotUseCase.CreateSlotCommand command = new DeliverySlotUseCase.CreateSlotCommand(
                zone.id(), slotDate, LocalTime.of(14, 0), LocalTime.of(17, 0),
                DeliverySlotType.STANDARD, 1, null, 0
        );
        DeliverySlot slot = slotUseCase.createSlot(command, "admin");

        UUID locId = UUID.randomUUID();
        when(locationLookupPort.findLocation(locId))
                .thenReturn(Optional.of(new DeliveryLocationLookupPort.LocationReference(locId, "LOC-1", "Dest Loc", "Address", 6.95, 79.85, true)));

        String n1 = "DEL-2026-" + String.format("%06d", (int) (Math.random() * 900000 + 100000));
        String n2 = "DEL-2026-" + String.format("%06d", (int) (Math.random() * 900000 + 100000));

        // Create 2 test orders
        DeliveryOrder order1 = DeliveryOrder.create(
                new DeliveryId(UUID.randomUUID()), new DeliveryNumber(n1), UUID.randomUUID(),
                UUID.randomUUID(), locId, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(OffsetDateTime.now(), OffsetDateTime.now().plusHours(4)),
                "Fragile", OffsetDateTime.now(), "admin"
        ).markReadyForAssignment(OffsetDateTime.now(), "admin");
        orderRepository.save(order1);

        DeliveryOrder order2 = DeliveryOrder.create(
                new DeliveryId(UUID.randomUUID()), new DeliveryNumber(n2), UUID.randomUUID(),
                UUID.randomUUID(), locId, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(OffsetDateTime.now(), OffsetDateTime.now().plusHours(4)),
                "Fragile", OffsetDateTime.now(), "admin"
        ).markReadyForAssignment(OffsetDateTime.now(), "admin");
        orderRepository.save(order2);

        // Assign order 1
        DeliverySlotReservation res1 = slotUseCase.assignDeliveryOrder(
                slot.getId(), new DeliverySlotUseCase.AssignSlotCommand(order1.id().value(), false, null), "admin"
        );
        assertNotNull(res1);
        assertEquals(DeliverySlotReservationStatus.ACTIVE, res1.getStatus());

        // Assign order 2 -> should fail due to capacity
        assertThrows(ConflictException.class, () -> slotUseCase.assignDeliveryOrder(
                slot.getId(), new DeliverySlotUseCase.AssignSlotCommand(order2.id().value(), false, null), "admin"
        ));

        // Assign order 2 with manager override -> succeeds
        DeliverySlotReservation overrideRes = slotUseCase.assignDeliveryOrder(
                slot.getId(), new DeliverySlotUseCase.AssignSlotCommand(order2.id().value(), true, "VIP customer escalation"), "admin"
        );
        assertNotNull(overrideRes);
        assertTrue(overrideRes.isOverride());

        // Release order 1
        DeliverySlotReservation released = slotUseCase.releaseReservation(slot.getId(), order1.id().value(), "admin");
        assertEquals(DeliverySlotReservationStatus.RELEASED, released.getStatus());
    }
}
