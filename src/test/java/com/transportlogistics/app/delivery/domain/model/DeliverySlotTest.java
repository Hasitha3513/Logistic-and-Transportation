package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeliverySlotTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 9, 15);
    private final OffsetDateTime now = OffsetDateTime.of(2026, 9, 15, 8, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void shouldCreateValidDeliverySlot() {
        DeliverySlot slot = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10,
                now.plusHours(1), 15, "admin", now
        );

        assertNotNull(slot);
        assertEquals(10, slot.getMaxCapacity());
        assertEquals(0, slot.getReservedCapacity());
        assertEquals(10, slot.getRemainingCapacity());
        assertEquals(DeliverySlotStatus.ACTIVE, slot.getStatus());
    }

    @Test
    void shouldRejectInvalidTimeWindow() {
        assertThrows(BusinessRuleException.class, () -> DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(12, 0), LocalTime.of(9, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        ));
    }

    @Test
    void shouldRejectInvalidCapacity() {
        assertThrows(BusinessRuleException.class, () -> DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 0, null, 0, "admin", now
        ));
    }

    @Test
    void shouldDetectOverlappingSlots() {
        DeliverySlot slot1 = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        );

        DeliverySlot slot2 = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(11, 0), LocalTime.of(14, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        );

        DeliverySlot adjacentSlot = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(12, 0), LocalTime.of(15, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        );

        assertTrue(slot1.overlapsWith(slot2));
        assertFalse(slot1.overlapsWith(adjacentSlot), "Adjacent half-open intervals should not overlap");
    }

    @Test
    void shouldReserveAndReleaseCapacity() {
        DeliverySlot slot = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 2, null, 0, "admin", now
        );

        DeliverySlot res1 = slot.reserve(false, null, "user", now);
        assertEquals(1, res1.getReservedCapacity());
        assertEquals(1, res1.getRemainingCapacity());

        DeliverySlot res2 = res1.reserve(false, null, "user", now);
        assertEquals(2, res2.getReservedCapacity());
        assertEquals(0, res2.getRemainingCapacity());

        assertThrows(ConflictException.class, () -> res2.reserve(false, null, "user", now));

        // Overbooking with override
        DeliverySlot overrideRes = res2.reserve(true, "Emergency VIP order", "admin", now);
        assertEquals(3, overrideRes.getReservedCapacity());

        // Release
        DeliverySlot released = overrideRes.release("admin", now);
        assertEquals(2, released.getReservedCapacity());
    }

    @Test
    void shouldRejectReducingCapacityBelowReserved() {
        DeliverySlot slot = DeliverySlot.create(
                UUID.randomUUID(), tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 5, null, 0, "admin", now
        );

        DeliverySlot res = slot.reserve(false, null, "user", now).reserve(false, null, "user", now);
        assertEquals(2, res.getReservedCapacity());

        assertThrows(ConflictException.class, () -> res.update(
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 1, null, 0, res.getVersion(), "admin", now
        ));
    }
}
