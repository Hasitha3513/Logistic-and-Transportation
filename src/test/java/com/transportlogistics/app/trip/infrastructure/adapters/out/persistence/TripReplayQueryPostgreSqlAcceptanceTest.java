package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.support.ReferenceFixtures;
import com.transportlogistics.app.trip.TripReplayQuery;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("postgres")
class TripReplayQueryPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired TripReplayQuery replay;

    @BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void resolvesScopeWithExactActualAndRouteFactsAndSafeTenantAbsence() {
        UUID vehicle = UUID.randomUUID();
        UUID trip = insertTrip(TENANT, vehicle, START, START.plusSeconds(3_600),
                "COMPLETED", UUID.randomUUID(), "REVISION:7");

        assertThat(replay.findReplayScope(TENANT, trip)).get().satisfies(scope -> {
            assertThat(scope.vehicleId()).isEqualTo(vehicle);
            assertThat(scope.actualStartTime()).isEqualTo(START);
            assertThat(scope.actualEndTime()).isEqualTo(START.plusSeconds(3_600));
            assertThat(scope.routeVersion()).isEqualTo("REVISION:7");
        });
        assertThat(replay.findReplayScope(UUID.randomUUID(), trip)).isEmpty();
        assertThat(replay.findReplayScope(TENANT, UUID.randomUUID())).isEmpty();
    }

    @Test
    void scopeRequiresActualVehicleAssignmentAndAllowsActiveOpenEnd() {
        UUID active = insertTrip(TENANT, UUID.randomUUID(), START, null,
                "IN_PROGRESS", null, null);
        UUID noVehicle = insertTrip(TENANT, null, START, null,
                "IN_PROGRESS", null, null);

        assertThat(replay.findReplayScope(TENANT, active)).get()
                .extracting(TripReplayQuery.TripReplayScope::actualEndTime).isNull();
        assertThat(replay.findReplayScope(TENANT, noVehicle)).isEmpty();
    }

    @Test
    void returnsAllOverlapsInStableHalfOpenOrderWithLifecycleAndTenantIsolation() {
        UUID vehicle = UUID.randomUUID();
        UUID otherVehicle = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        insertTenant(otherTenant);
        UUID spanning = insertTrip(TENANT, vehicle, START.minusSeconds(60), START.plusSeconds(600),
                "IN_PROGRESS", null, null);
        UUID tiedSecond = new UUID(0, 2);
        UUID tiedFirst = new UUID(0, 1);
        insertTrip(tiedSecond, TENANT, vehicle, START.plusSeconds(10), START.plusSeconds(20), "COMPLETED");
        insertTrip(tiedFirst, TENANT, vehicle, START.plusSeconds(10), START.plusSeconds(30), "COMPLETED");
        insertTrip(TENANT, vehicle, START.minusSeconds(100), START, "COMPLETED", null, null);
        insertTrip(TENANT, vehicle, START.plusSeconds(600), START.plusSeconds(700), "IN_PROGRESS", null, null);
        insertTrip(TENANT, vehicle, START.plusSeconds(30), START.plusSeconds(40), "CANCELLED", null, null);
        insertTrip(TENANT, vehicle, START.plusSeconds(40), START.plusSeconds(50), "REJECTED", null, null);
        insertTrip(TENANT, otherVehicle, START.plusSeconds(10), START.plusSeconds(20), "COMPLETED", null, null);
        insertTrip(otherTenant, vehicle, START.plusSeconds(10), START.plusSeconds(20), "COMPLETED", null, null);

        var intervals = replay.findAssignmentsOverlapping(
                TENANT, vehicle, START, START.plusSeconds(600));
        assertThat(intervals).extracting(TripReplayQuery.VehicleTripAssignmentInterval::tripId)
                .containsExactly(spanning, tiedFirst, tiedSecond);
    }

    @Test
    void v92IndexSupportsBoundedTenantVehicleOverlapQuery() {
        UUID vehicle = UUID.randomUUID();
        UUID noiseVehicle = UUID.randomUUID();
        for (int index = 0; index < 1_000; index++) {
            UUID target = index % 100 == 0 ? vehicle : noiseVehicle;
            insertTrip(TENANT, target, START.plusSeconds(index), START.plusSeconds(index + 30L),
                    "COMPLETED", null, null);
        }
        jdbc.execute("ANALYZE trip");
        String plan = String.join("\n", jdbc.queryForList("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT id, vehicle_id, actual_start_time, actual_end_time, route_id, route_version, status
                FROM trip
                WHERE tenant_id = ? AND vehicle_id = ? AND actual_start_time IS NOT NULL
                  AND actual_start_time < ? AND (actual_end_time IS NULL OR actual_end_time > ?)
                  AND status NOT IN ('CANCELLED', 'REJECTED')
                ORDER BY actual_start_time ASC, id ASC LIMIT 2001
                """, String.class, TENANT, vehicle, OffsetDateTime.ofInstant(START.plusSeconds(2_000),
                java.time.ZoneOffset.UTC), OffsetDateTime.ofInstant(START, java.time.ZoneOffset.UTC)));

        assertThat(plan).contains("idx_trip_tenant_vehicle_source_assignment");
        assertThat(plan).doesNotContain("Seq Scan on trip");
    }

    private UUID insertTrip(UUID tenant, UUID vehicle, Instant from, Instant to,
                            String status, UUID route, String revision) {
        UUID id = UUID.randomUUID();
        insertTrip(id, tenant, vehicle, from, to, status, route, revision);
        return id;
    }

    private void insertTrip(UUID id, UUID tenant, UUID vehicle, Instant from, Instant to, String status) {
        insertTrip(id, tenant, vehicle, from, to, status, null, null);
    }

    private void insertTrip(UUID id, UUID tenant, UUID vehicle, Instant from, Instant to,
                            String status, UUID route, String revision) {
        UUID origin = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, origin, destination);
        ensureVehicle(vehicle);
        ensureRoute(tenant, route, origin, destination);
        jdbc.update("""
                INSERT INTO trip(id, trip_number, tenant_id, route_id, route_version, priority, status,
                  origin_location_id, destination_location_id, requested_start_time, requested_end_time,
                  vehicle_id, actual_start_time, actual_end_time, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'NORMAL', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "REPLAY-" + id.toString().substring(24), tenant, route, revision, status,
                origin, destination,
                OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
                OffsetDateTime.ofInstant(to == null ? from.plusSeconds(3_600) : to, java.time.ZoneOffset.UTC),
                vehicle, OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
                to == null ? null : OffsetDateTime.ofInstant(to, java.time.ZoneOffset.UTC),
                OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
                OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC));
    }

    private void insertTenant(UUID tenant) {
        jdbc.update("""
                INSERT INTO tenant(tenant_id, tenant_code, tenant_name, default_currency,
                  default_time_zone, status, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, ?, 'Replay test tenant', 'LKR', 'UTC', 'ACTIVE', now(), 'test', now(), 'test', 0)
                """, tenant, "REPLAY-" + tenant.toString().substring(0, 12));
    }

    private void ensureVehicle(UUID vehicle) {
        if (vehicle != null && jdbc.queryForObject(
                "SELECT count(*) FROM vehicle WHERE id = ?", Integer.class, vehicle) == 0) {
            ReferenceFixtures.vehicleReference(jdbc, vehicle);
        }
    }

    private void ensureRoute(UUID tenant, UUID route, UUID origin, UUID destination) {
        if (route != null && jdbc.queryForObject(
                "SELECT count(*) FROM route WHERE id = ?", Integer.class, route) == 0) {
            jdbc.update("""
                    INSERT INTO route(id, code, name, origin_location_id, destination_location_id,
                      active, tenant_id)
                    VALUES (?, ?, 'Replay test route', ?, ?, true, ?)
                    """, route, "REPLAY-" + route.toString().substring(0, 12), origin, destination, tenant);
        }
    }
}
