package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FreightOrderPersistenceIntegrationTest {
    @Autowired FreightOrderUseCase orders;
    @Autowired JdbcTemplate jdbc;
    private UUID customerId, originId, destinationId;

    @BeforeEach
    void seedReferences() {
        customerId = UUID.randomUUID(); originId = UUID.randomUUID(); destinationId = UUID.randomUUID();
        jdbc.update("INSERT INTO customer (id, code, name, active) VALUES (?, ?, ?, ?)", customerId, "FC-" + customerId.toString().substring(0, 8), "Freight Customer", true);
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, ?)", originId, "FO-" + originId.toString().substring(0, 8), "Origin", true);
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, ?)", destinationId, "FD-" + destinationId.toString().substring(0, 8), "Destination", true);
    }

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM freight_order_line"); jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM location WHERE id IN (?, ?)", originId, destinationId);
        jdbc.update("DELETE FROM customer WHERE id = ?", customerId);
    }

    @Test
    void persistsSearchesUpdatesAndEnforcesOptimisticVersion() {
        var created = orders.create(command(), "integration-user");
        assertTrue(created.orderNumber().startsWith("FO-2026-")); assertEquals(0, created.version());
        assertEquals("Pallets", orders.get(created.id()).lines().getFirst().description());
        var page = orders.search(new FreightOrderUseCase.SearchQuery(created.orderNumber(), customerId, null, null, 0, 20, "orderNumber", "asc"));
        assertEquals(1, page.totalElements());

        var updated = orders.update(created.id(), new FreightOrderUseCase.UpdateCommand(created.version(), null, null, null,
                null, null, null, "URGENT", "Keep upright", null), "editor");
        assertEquals(1, updated.version()); assertEquals("URGENT", updated.priority()); assertEquals("editor", updated.updatedBy());
        assertThrows(ConflictException.class, () -> orders.update(created.id(), new FreightOrderUseCase.UpdateCommand(
                0L, null, null, null, null, null, null, "LOW", null, null), "stale-editor"));
    }

    private FreightOrderUseCase.CreateCommand command() {
        return new FreightOrderUseCase.CreateCommand(customerId, originId, destinationId,
                OffsetDateTime.parse("2026-09-01T08:00:00Z"), OffsetDateTime.parse("2026-09-02T08:00:00Z"),
                "STANDARD", "NORMAL", null, List.of(new FreightOrderUseCase.LineCommand(null, "Pallets", new BigDecimal("4"))));
    }
}
