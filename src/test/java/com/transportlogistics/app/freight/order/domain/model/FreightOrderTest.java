package com.transportlogistics.app.freight.order.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FreightOrderTest {
    @Test
    void createsOrderWithNormalizedCodesAndImmutableLines() {
        var lines = new java.util.ArrayList<>(List.of(new FreightOrderLine(UUID.randomUUID(), " Pallets ", BigDecimal.ONE)));
        FreightOrder order = order(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse("2026-09-01T08:00:00Z"),
                OffsetDateTime.parse("2026-09-02T08:00:00Z"), lines);
        lines.clear();
        assertEquals("STANDARD", order.serviceLevel());
        assertEquals("NORMAL", order.priority());
        assertEquals("Pallets", order.lines().getFirst().description());
        assertEquals(1, order.lines().size());
    }

    @Test
    void rejectsSameLocationsInvalidWindowAndMissingLines() {
        UUID location = UUID.randomUUID();
        OffsetDateTime pickup = OffsetDateTime.parse("2026-09-01T08:00:00Z");
        assertThrows(BusinessRuleException.class, () -> order(location, location, pickup, pickup.plusHours(1), lines()));
        assertThrows(BusinessRuleException.class, () -> order(UUID.randomUUID(), UUID.randomUUID(), pickup, pickup.minusMinutes(1), lines()));
        assertThrows(BusinessRuleException.class, () -> order(UUID.randomUUID(), UUID.randomUUID(), pickup, pickup.plusHours(1), List.of()));
    }

    @Test
    void rejectsInvalidShipmentLine() {
        assertThrows(BusinessRuleException.class, () -> new FreightOrderLine(UUID.randomUUID(), " ", BigDecimal.ONE));
        assertThrows(BusinessRuleException.class, () -> new FreightOrderLine(UUID.randomUUID(), "Cargo", BigDecimal.ZERO));
    }

    private FreightOrder order(UUID origin, UUID destination, OffsetDateTime pickup, OffsetDateTime delivery,
                               List<FreightOrderLine> lines) {
        var now = OffsetDateTime.parse("2026-08-25T00:00:00Z");
        return new FreightOrder(UUID.randomUUID(), "FO-2026-000001", UUID.randomUUID(), origin, destination,
                pickup, delivery, " standard ", " normal ", null, lines, 0, now, now, "tester", "tester");
    }
    private List<FreightOrderLine> lines() { return List.of(new FreightOrderLine(UUID.randomUUID(), "Cargo", BigDecimal.ONE)); }
}
