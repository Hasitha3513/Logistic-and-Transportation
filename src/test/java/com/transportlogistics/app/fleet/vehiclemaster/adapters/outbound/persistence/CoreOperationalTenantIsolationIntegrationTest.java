package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound.persistence;

import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.trip.TripReportingQuery;
import com.transportlogistics.app.trip.infrastructure.adapters.out.persistence.TripEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=false",
        "app.dev.sample-data.enabled=false"
})
class CoreOperationalTenantIsolationIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("a0000000-0000-4000-8000-000000000004");
    private static final UUID TENANT_B = UUID.fromString("b0000000-0000-4000-8000-000000000004");
    private static final UUID ACTOR = UUID.fromString("c0000000-0000-4000-8000-000000000004");

    private final UUID categoryA = UUID.randomUUID();
    private final UUID categoryB = UUID.randomUUID();
    private final UUID typeA = UUID.randomUUID();
    private final UUID typeB = UUID.randomUUID();
    private final UUID originA = UUID.randomUUID();
    private final UUID destinationA = UUID.randomUUID();
    private final UUID originB = UUID.randomUUID();
    private final UUID destinationB = UUID.randomUUID();
    private final UUID vehicleA = UUID.randomUUID();
    private final UUID vehicleB = UUID.randomUUID();
    private final UUID driverB = UUID.randomUUID();
    private final UUID documentB = UUID.randomUUID();
    private final UUID routeB = UUID.randomUUID();
    private final UUID tripA = UUID.randomUUID();
    private final UUID tripB = UUID.randomUUID();

    @Autowired VehicleJpaRepository vehicles;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired TripReportingQuery tripReporting;

    @BeforeEach
    void seedTwoTenants() {
        insertReferenceData(TENANT_A, categoryA, typeA, originA, destinationA);
        insertReferenceData(TENANT_B, categoryB, typeB, originB, destinationB);
        insertVehicle(TENANT_A, vehicleA, categoryA, typeA);
        insertVehicle(TENANT_B, vehicleB, categoryB, typeB);
        jdbc.update("INSERT INTO driver (id, employee_number, first_name, last_name, status, active, tenant_id) VALUES (?, 'P0-EMP', 'Tenant', 'B', 'AVAILABLE', TRUE, ?)", driverB, TENANT_B);
        jdbc.update("INSERT INTO vehicle_document (id, vehicle_id, document_type, document_number, mandatory_for_dispatch, status, active, created_at, updated_at, created_by, updated_by, tenant_id) VALUES (?, ?, 'INSURANCE', 'P0-DOC', TRUE, 'ACTIVE', TRUE, NOW(), NOW(), 'test', 'test', ?)", documentB, vehicleB, TENANT_B);
        jdbc.update("INSERT INTO route (id, code, name, origin_location_id, destination_location_id, active, tenant_id) VALUES (?, 'P0-ROUTE', 'Tenant B Route', ?, ?, TRUE, ?)", routeB, originB, destinationB, TENANT_B);
        insertTrip(TENANT_A, tripA, "P0-TRIP", originA, destinationA);
        insertTrip(TENANT_B, tripB, "P0-TRIP", originB, destinationB);
    }

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM vehicle_document WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM trip WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM route WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM driver WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM vehicle WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM vehicle_type WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM vehicle_category WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM location WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
    }

    @Test
    void tenantACannotReadUpdateOrDeleteTenantBVehicleAndCanUseItsOwnVehicle() {
        inTenant(TENANT_A, () -> {
            assertThat(vehicles.findById(vehicleA)).isPresent();
            assertThat(vehicles.findById(vehicleB)).isEmpty();
            assertThat(vehicles.findById(UUID.randomUUID())).isEmpty();
            assertThat(vehicles.findAll()).extracting(VehicleEntity::getId).contains(vehicleA).doesNotContain(vehicleB);
            assertThat(vehicles.findByIdForUpdate(vehicleB)).isEmpty();
            vehicles.deleteById(vehicleB);
            return null;
        });

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehicle WHERE id = ? AND tenant_id = ?", Long.class, vehicleB, TENANT_B)).isOne();
    }

    @Test
    void tenantACannotReadTenantBDriverTripRouteDocumentOrModifyTenantBTrip() {
        inTenant(TENANT_A, () -> {
            assertThat(count("DriverEntity", driverB)).isZero();
            assertThat(count("RouteEntity", routeB)).isZero();
            assertThat(count("VehicleDocumentEntity", documentB)).isZero();

            var foreignTrip = entityManager.createQuery("select t from TripEntity t where t.id = :id", TripEntity.class)
                    .setParameter("id", tripB).getResultStream().findFirst();
            assertThat(foreignTrip).isEmpty();
            assertThat(tripReporting.findAllTripSummaries()).extracting(com.transportlogistics.app.trip.TripReportItem::tripNumber)
                    .containsExactly("P0-TRIP");
            return null;
        });

        assertThat(jdbc.queryForObject("SELECT notes FROM trip WHERE id = ?", String.class, tripB)).isNull();
    }

    private long count(String entityName, UUID id) {
        return entityManager.createQuery("select count(e) from " + entityName + " e where e.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
    }

    private void insertReferenceData(UUID tenantId, UUID categoryId, UUID typeId, UUID originId, UUID destinationId) {
        jdbc.update("INSERT INTO vehicle_category (id, code, name, active, tenant_id) VALUES (?, 'P0-CAT', 'P0 Category', TRUE, ?)", categoryId, tenantId);
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, active, tenant_id) VALUES (?, ?, 'P0-TYPE', 'P0 Type', TRUE, ?)", typeId, categoryId, tenantId);
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, 'P0-ORIGIN', 'Origin', TRUE, ?)", originId, tenantId);
        jdbc.update("INSERT INTO location (id, code, name, active, tenant_id) VALUES (?, 'P0-DEST', 'Destination', TRUE, ?)", destinationId, tenantId);
    }

    private void insertVehicle(UUID tenantId, UUID vehicleId, UUID categoryId, UUID typeId) {
        jdbc.update("INSERT INTO vehicle (id, registration_number, category_id, type_id, ownership_type, operational_status, active, tenant_id) VALUES (?, 'P0-REG', ?, ?, 'OWNED', 'AVAILABLE', TRUE, ?)", vehicleId, categoryId, typeId, tenantId);
    }

    private void insertTrip(UUID tenantId, UUID id, String number, UUID origin, UUID destination) {
        OffsetDateTime start = OffsetDateTime.parse("2026-09-01T08:00:00Z");
        jdbc.update("INSERT INTO trip (id, trip_number, priority, status, origin_location_id, destination_location_id, requested_start_time, requested_end_time, created_at, updated_at, tenant_id) VALUES (?, ?, 'NORMAL', 'DRAFT', ?, ?, ?, ?, ?, ?, ?)",
                id, number, origin, destination, start, start.plusHours(8), start, start, tenantId);
    }

    private <T> T inTenant(UUID tenantId, Supplier<T> work) {
        return tenantContexts.within(new TenantExecutionContext(tenantId, ACTOR, "p0-04-test", UUID.randomUUID().toString()),
                () -> new TransactionTemplate(transactions).execute(status -> work.get()));
    }
}
