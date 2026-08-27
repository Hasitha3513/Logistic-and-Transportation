package com.transportlogistics.app.freight.manifest.adapters.outbound.persistence;

import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CargoManifestPersistenceIntegrationTest {

    @Autowired
    CargoManifestUseCase manifests;

    @Autowired
    FreightOrderUseCase orders;

    @Autowired
    JdbcTemplate jdbc;

    UUID customer;
    UUID origin;
    UUID destination;

    @BeforeEach
    void seed() {
        customer = UUID.randomUUID();
        origin = UUID.randomUUID();
        destination = UUID.randomUUID();
        jdbc.update("INSERT INTO customer (id,code,name,active) VALUES (?,?,?,TRUE)",
                customer, "MC-" + shortId(customer), "Manifest Customer");
        jdbc.update("INSERT INTO location (id,code,name,active) VALUES (?,?,?,TRUE)",
                origin, "MO-" + shortId(origin), "Origin");
        jdbc.update("INSERT INTO location (id,code,name,active) VALUES (?,?,?,TRUE)",
                destination, "MD-" + shortId(destination), "Destination");
    }

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM cargo_manifest_item");
        jdbc.update("DELETE FROM cargo_manifest");
        jdbc.update("DELETE FROM freight_order_line");
        jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM location WHERE id IN (?,?)", origin, destination);
        jdbc.update("DELETE FROM customer WHERE id=?", customer);
    }

    @Test
    void roundTripsItemsPagingFinalizationAuditAndVersion() {
        var order = createOrder(BigDecimal.TEN);
        var manifest = manifests.create(new CargoManifestUseCase.CreateCommand(order.id()), "creator");
        var item = itemCommand(
                manifest.version(), order.lines().getFirst().id(), BigDecimal.TEN, false, false
        );

        var added = manifests.addItem(manifest.id(), item, "editor");

        assertEquals(1, added.items().size());
        assertEquals(1, added.version());
        assertEquals(Boolean.FALSE, added.items().getFirst().fragile());
        assertEquals(Boolean.FALSE, added.items().getFirst().temperatureSensitive());
        assertTrue(manifests.validate(added.id()).ready());
        assertEquals(1, manifests.search(new CargoManifestUseCase.SearchQuery(
                added.manifestNumber(), order.id(), false, 0, 10, "manifestNumber", "asc"
        )).totalElements());

        var finalized = manifests.finalizeManifest(added.id(), added.version(), "finalizer");

        assertTrue(finalized.finalized());
        assertEquals("finalizer", finalized.finalizedBy());
        assertThrows(ConflictException.class, () -> manifests.update(
                finalized.id(), new CargoManifestUseCase.UpdateCommand(finalized.version()), "editor"
        ));
    }

    @Test
    void preservesTrueFalseAndUnknownSpecialCargoClassifications() {
        var order = createOrder(new BigDecimal("3"));
        var manifest = manifests.create(new CargoManifestUseCase.CreateCommand(order.id()), "creator");
        UUID lineId = order.lines().getFirst().id();

        var first = manifests.addItem(manifest.id(),
                itemCommand(manifest.version(), lineId, BigDecimal.ONE, true, true), "editor");
        var second = manifests.addItem(manifest.id(),
                itemCommand(first.version(), lineId, BigDecimal.ONE, false, false), "editor");
        var third = manifests.addItem(manifest.id(),
                itemCommand(second.version(), lineId, BigDecimal.ONE, null, null), "editor");

        var reloaded = manifests.get(third.id());

        assertEquals(Boolean.TRUE, reloaded.items().get(0).fragile());
        assertEquals(Boolean.TRUE, reloaded.items().get(0).temperatureSensitive());
        assertEquals(Boolean.FALSE, reloaded.items().get(1).fragile());
        assertEquals(Boolean.FALSE, reloaded.items().get(1).temperatureSensitive());
        assertNull(reloaded.items().get(2).fragile());
        assertNull(reloaded.items().get(2).temperatureSensitive());
        assertTrue(manifests.validate(reloaded.id()).failures().stream()
                .anyMatch(failure -> "SPECIAL_CARGO_CLASSIFICATION_MISSING".equals(failure.code())));
    }

    @Test
    void migrationCreatesNullableColumnsWithoutHistoricalBackfill() {
        assertEquals("YES", nullable("FRAGILE"));
        assertEquals("YES", nullable("TEMPERATURE_SENSITIVE"));
    }

    private FreightOrder createOrder(BigDecimal quantity) {
        return orders.create(new FreightOrderUseCase.CreateCommand(
                customer,
                origin,
                destination,
                OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                OffsetDateTime.parse("2026-09-02T00:00:00Z"),
                "STANDARD",
                "NORMAL",
                null,
                List.of(new FreightOrderUseCase.LineCommand(null, "Cargo", quantity))
        ), "creator");
    }

    private CargoManifestUseCase.ItemCommand itemCommand(Long version, UUID lineId, BigDecimal quantity,
                                                          Boolean fragile, Boolean temperatureSensitive) {
        return new CargoManifestUseCase.ItemCommand(
                version, lineId, "Cargo", quantity, "Wrapped", "SUPPLIER.CODE",
                false, null, false, null, null, fragile, temperatureSensitive
        );
    }

    private String nullable(String columnName) {
        return jdbc.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE LOWER(table_name) = 'cargo_manifest_item'
                  AND LOWER(column_name) = LOWER(?)
                """, String.class, columnName);
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
