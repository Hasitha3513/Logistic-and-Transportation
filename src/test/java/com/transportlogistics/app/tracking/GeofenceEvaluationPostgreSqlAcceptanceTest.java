package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GeofencePositionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GeofenceEvaluationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired GeofenceEvaluationUseCase evaluator;
    @Autowired GeofencePositionRepositoryPort positions;
    @Autowired GeofenceRepositoryPort geofences;
    @Autowired VehicleGeofenceStateRepositoryPort states;
    @Autowired GeofenceTransitionRepositoryPort transitions;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantContextExecutor tenantContexts;

    @Test
    void outsideThenTwoInsidePositionsProduceOneDurableUnauthorizedTransition() {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        evaluate(position(fixture, now.minusSeconds(3), 20, 20), now);
        evaluate(position(fixture, now.minusSeconds(2), 2, 2), now);
        evaluate(position(fixture, now.minusSeconds(1), 3, 2), now);

        assertThat(states.find(fixture.tenant(), fixture.vehicle(), fixture.geofenceId(), 0, 10))
                .singleElement().satisfies(state -> assertThat(state.stableState().name())
                        .isEqualTo("INSIDE"));
        assertThat(transitions.find(fixture.tenant(), fixture.geofenceId(), fixture.vehicle(),
                null, null, null, null, 10, false)).singleElement()
                .extracting(transition -> transition.transitionType())
                .isEqualTo(GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED);
        var outbox = jdbc.queryForMap("""
                SELECT event_id,tenant_id,event_type,event_version,aggregate_type,payload::text payload
                FROM integration_outbox_event
                WHERE tenant_id=? AND consumer_name='geofence-transition-notification-bridge'
                """, fixture.tenant());
        assertThat(outbox).containsEntry("tenant_id", fixture.tenant())
                .containsEntry("event_type", "VEHICLE_GEOFENCE_TRANSITIONED_V1")
                .containsEntry("event_version", 1)
                .containsEntry("aggregate_type", "GEOFENCE_TRANSITION");
        assertThat((String) outbox.get("payload"))
                .contains("\"geofenceId\"", "\"vehicleId\"", "\"locationId\": null",
                        "\"geofenceType\"", "\"transition\"", "\"severity\"",
                        "\"sourceTimestamp\"", "\"definitionVersion\"")
                .doesNotContain("latitude", "longitude", "polygon", "device", "provider",
                        "imei", "credential", "driver", "customer", "address");
    }

    @Test
    void bboxOutsideInitializationIsTenantScopedAndLaterSupportsExit() {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        evaluate(position(fixture, now.minusSeconds(3), 2, 2), now);
        evaluate(position(fixture, now.minusSeconds(2), 20, 20), now);
        evaluate(position(fixture, now.minusSeconds(1), 21, 20), now);

        assertThat(transitions.find(fixture.tenant(), fixture.geofenceId(), fixture.vehicle(),
                null, null, null, null, 10, false)).singleElement()
                .extracting(transition -> transition.transitionType())
                .isEqualTo(GeofenceTransitionType.EXITED);
        assertThat(states.find(UUID.randomUUID(), fixture.vehicle(), fixture.geofenceId(), 0, 10))
                .isEmpty();
    }

    private void evaluate(UUID positionId, Instant evaluatedAt) {
        UUID tenantId = tenant(positionId);
        tenantContexts.within(new TenantExecutionContext(
                tenantId, ACTOR, "geofence-acceptance", "geofence-acceptance"), () ->
                evaluator.evaluate(positions.find(tenantId, positionId).orElseThrow(), evaluatedAt));
    }

    private UUID tenant(UUID positionId) {
        return jdbc.queryForObject("SELECT tenant_id FROM tracking_position WHERE id=?",
                UUID.class, positionId);
    }

    private UUID position(Fixture fixture, Instant source, double longitude, double latitude) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position(
                 id,tenant_id,device_id,vehicle_id,provider_alias,dedupe_identity,payload_hash,
                 source_timestamp,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?,?,?,
                 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                 ?,?,?,?, 'UNKNOWN','TRUSTED','GOOD','IN_ORDER','TEST','1')
                """, id, fixture.tenant(), fixture.device(), fixture.vehicle(), "FIXTURE", hex(id),
                Timestamp.from(source), Timestamp.from(source.plusMillis(10)), latitude, longitude);
        return id;
    }

    private Fixture fixture(Instant now) {
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,
                 registered_by,version,created_at,updated_at)
                VALUES(?,?,?,?,'ACTIVE',?,?,0,?,?)
                """, device, tenant, "device-" + device, "FIXTURE", Timestamp.from(now), ACTOR,
                Timestamp.from(now), Timestamp.from(now));
        Geofence geofence = Geofence.draft(UUID.randomUUID(), tenant, "Unauthorized",
                GeofenceType.UNAUTHORIZED_ZONE,
                GeofencePolygon.of(List.of(new Wgs84Coordinate(0, 0),
                        new Wgs84Coordinate(10, 0), new Wgs84Coordinate(0, 10))), null,
                GeofenceAlertPolicy.unauthorizedZone(), now.minusSeconds(60), ACTOR);
        geofences.save(geofence, 0);
        geofence.activate(now.minusSeconds(59), ACTOR);
        geofences.save(geofence, 0);
        return new Fixture(tenant, device, vehicle, geofence.id());
    }

    private static String hex(UUID value) {
        return value.toString().replace("-", "") + value.toString().replace("-", "");
    }

    private record Fixture(UUID tenant, UUID device, UUID vehicle, UUID geofenceId) {
    }
}
