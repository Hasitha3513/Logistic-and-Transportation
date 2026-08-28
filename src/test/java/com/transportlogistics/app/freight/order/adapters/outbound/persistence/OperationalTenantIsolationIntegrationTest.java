package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.trip.TripReportingQuery;
import com.transportlogistics.app.freight.FreightReportingQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=false",
        "app.dev.sample-data.enabled=false"
})
class OperationalTenantIsolationIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID ACTOR = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    @Autowired FreightOrderJpaRepository freightOrders;
    @Autowired TripReportingQuery tripReporting;
    @Autowired FreightReportingQuery freightReporting;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM freight_order_line WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM freight_order WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM trip WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM customer WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM location WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
    }

    @Test
    void freightAndReportingQueriesAreIsolatedByCurrentTenant() {
        var orderA = order("FO-TENANT-A");
        var orderB = order("FO-TENANT-B");
        seedReferences(orderA, TENANT_A);
        seedReferences(orderB, TENANT_B);

        inTenant(TENANT_A, () -> freightOrders.saveAndFlush(orderA));
        inTenant(TENANT_B, () -> freightOrders.saveAndFlush(orderB));

        assertEquals(1L, inTenant(TENANT_A, freightOrders::count).longValue());
        assertTrue(inTenant(TENANT_A, () -> freightOrders.findById(orderB.getId())).isEmpty());
        assertEquals(TENANT_A, jdbc.queryForObject(
                "SELECT tenant_id FROM freight_order WHERE id = ?", UUID.class, orderA.getId()));

        var criteria = new FreightReportingQuery.FreightReportCriteria(
                OffsetDateTime.parse("2026-08-01T00:00:00Z"), OffsetDateTime.parse("2026-10-01T00:00:00Z"),
                null, null, null, null, null, null, null, null, null);
        var freightA = inTenant(TENANT_A, () -> freightReporting.shipments(criteria, PageRequest.of(0, 20)));
        var freightB = inTenant(TENANT_B, () -> freightReporting.shipments(criteria, PageRequest.of(0, 20)));
        assertEquals(java.util.List.of("FO-TENANT-A"), freightA.map(item -> item.orderNumber()).toList());
        assertEquals(java.util.List.of("FO-TENANT-B"), freightB.map(item -> item.orderNumber()).toList());
        assertEquals(1L, inTenant(TENANT_A, () -> freightReporting.summary(criteria)).freightOrders());

        insertTrip(TENANT_A, "TRIP-TENANT-A");
        insertTrip(TENANT_B, "TRIP-TENANT-B");

        var reportsA = inTenant(TENANT_A, tripReporting::findAllTripSummaries);
        var reportsB = inTenant(TENANT_B, tripReporting::findAllTripSummaries);
        assertEquals(java.util.List.of("TRIP-TENANT-A"), reportsA.stream().map(item -> item.tripNumber()).toList());
        assertEquals(java.util.List.of("TRIP-TENANT-B"), reportsB.stream().map(item -> item.tripNumber()).toList());
    }

    private FreightOrderEntity order(String number) {
        var now = OffsetDateTime.parse("2026-09-01T08:00:00Z");
        var order = new FreightOrderEntity();
        order.setId(UUID.randomUUID());
        order.setOrderNumber(number);
        order.setCustomerId(UUID.randomUUID());
        order.setOriginLocationId(UUID.randomUUID());
        order.setDestinationLocationId(UUID.randomUUID());
        order.setRequestedPickupAt(now);
        order.setRequestedDeliveryAt(now.plusDays(1));
        order.setServiceLevel("STANDARD");
        order.setPriority("NORMAL");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setCreatedBy("tenant-test");
        order.setUpdatedBy("tenant-test");
        return order;
    }

    private void insertTrip(UUID tenantId, String tripNumber) {
        var start = OffsetDateTime.parse("2026-09-01T08:00:00Z");
        var originId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, ?, 'Trip Origin', TRUE, ?)",
                originId, "TO-" + originId.toString().substring(0, 8), tenantId);
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, ?, 'Trip Destination', TRUE, ?)",
                destinationId, "TD-" + destinationId.toString().substring(0, 8), tenantId);
        jdbc.update("""
                INSERT INTO trip (id, trip_number, priority, status, origin_location_id, destination_location_id,
                    requested_start_time, requested_end_time, created_at, updated_at, tenant_id)
                VALUES (?, ?, 'NORMAL', 'DRAFT', ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), tripNumber, originId, destinationId, start, start.plusHours(8),
                start, start, tenantId);
    }

    private void seedReferences(FreightOrderEntity order, UUID tenantId) {
        var suffix = order.getId().toString().substring(0, 8);
        jdbc.update("INSERT INTO customer (id, code, name, active, tenant_id) VALUES (?, ?, 'Tenant Customer', TRUE, ?)",
                order.getCustomerId(), "C-" + suffix, tenantId);
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, ?, 'Origin', TRUE, ?)",
                order.getOriginLocationId(), "O-" + suffix, tenantId);
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, ?, 'Destination', TRUE, ?)",
                order.getDestinationLocationId(), "D-" + suffix, tenantId);
    }

    private <T> T inTenant(UUID tenantId, Supplier<T> work) {
        return tenantContexts.within(new TenantExecutionContext(tenantId, ACTOR, "tenant-test", UUID.randomUUID().toString()),
                () -> new TransactionTemplate(transactions).execute(status -> work.get()));
    }
}
