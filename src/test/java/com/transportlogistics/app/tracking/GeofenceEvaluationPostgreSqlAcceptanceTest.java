package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.system.infrastructure.adapters.in.events.GeofenceTransitionNotificationBridge;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
    @Autowired DurableEventWorker outboxWorker;
    @Autowired GeofenceTransitionNotificationBridge notificationBridge;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    @Test
    void cleanV1ToV79TransitionCreatesOneTenantSafeIdempotentInAppNotification() {
        UUID tenantA = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
        UUID tenantB = UUID.fromString("49000000-0000-0000-0000-000000000002");
        migrateCleanSchemaWithDispatcherFixtures(tenantA, tenantB);
        Instant now = Instant.now();
        Fixture fixture = fixture(now, tenantA);

        UUID initial = position(fixture, now.minusSeconds(3), 20, 20);
        UUID pending = position(fixture, now.minusSeconds(2), 2, 2);
        UUID confirming = position(fixture, now.minusSeconds(1), 3, 2);
        evaluate(initial, now);
        assertThat(count("tracking_geofence_transition", tenantA)).isZero();
        assertThat(count("integration_outbox_event", tenantA)).isZero();
        evaluate(pending, now);
        assertThat(count("tracking_geofence_transition", tenantA)).isZero();
        assertThat(count("integration_outbox_event", tenantA)).isZero();
        evaluate(confirming, now);

        var transition = transitions.find(tenantA, fixture.geofenceId(), fixture.vehicle(),
                null, null, null, null, 10, false).getFirst();
        assertThat(transition.transitionType()).isEqualTo(GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED);
        assertThat(transition.severity().name()).isEqualTo("HIGH");
        Map<String, Object> outbox = jdbc.queryForMap("""
                SELECT event_id,tenant_id,event_type,event_version,consumer_name,aggregate_type,
                       aggregate_id,occurred_at,payload::text payload,status
                FROM integration_outbox_event WHERE tenant_id=? AND event_id=?
                """, tenantA, transition.transitionId());
        assertThat(outbox).containsEntry("event_id", transition.transitionId())
                .containsEntry("tenant_id", tenantA)
                .containsEntry("event_type", VehicleGeofenceTransitionedV1.EVENT_TYPE)
                .containsEntry("event_version", 1)
                .containsEntry("consumer_name", VehicleGeofenceTransitionedV1.CONSUMER)
                .containsEntry("aggregate_type", "GEOFENCE_TRANSITION")
                .containsEntry("aggregate_id", transition.transitionId())
                .containsEntry("status", "PENDING");
        Object occurredAt = outbox.get("occurred_at");
        Instant persistedOccurredAt = occurredAt instanceof OffsetDateTime offsetDateTime
                ? offsetDateTime.toInstant()
                : ((Timestamp) occurredAt).toInstant();
        assertThat(persistedOccurredAt).isEqualTo(transition.sourceTimestamp());
        String payload = (String) outbox.get("payload");
        assertThat(jdbc.queryForList("""
                SELECT jsonb_object_keys(payload) FROM integration_outbox_event
                WHERE tenant_id=? AND event_id=? ORDER BY 1
                """, String.class, tenantA, transition.transitionId()))
                .containsExactly("definitionVersion", "geofenceId", "geofenceType", "locationId",
                        "severity", "sourceTimestamp", "transition", "vehicleId");
        assertThat(payload).doesNotContainIgnoringCase("latitude", "longitude", "polygon", "device",
                "provider", "imei", "driver", "customer", "credential", "rawTelemetry");

        assertThat(jdbc.queryForMap("""
                SELECT recipient_type,recipient_value,channel,template_code
                FROM notification_rule WHERE tenant_id=? AND event_type=?
                """, tenantA, VehicleGeofenceTransitionedV1.EVENT_TYPE))
                .containsEntry("recipient_type", "ROLE")
                .containsEntry("recipient_value", "DISPATCHER")
                .containsEntry("channel", "IN_APP")
                .containsEntry("template_code", VehicleGeofenceTransitionedV1.EVENT_TYPE);
        assertThat(jdbc.queryForMap("""
                SELECT channel,version,active FROM notification_template
                WHERE event_type=?
                """, VehicleGeofenceTransitionedV1.EVENT_TYPE))
                .containsEntry("channel", "IN_APP").containsEntry("version", 1).containsEntry("active", true);

        withinTenant(tenantA, outboxWorker::processDue);
        assertNotification(tenantA, tenantB, transition.transitionId());

        withinTenant(tenantA, outboxWorker::processDue);
        VehicleGeofenceTransitionedV1 replay = new VehicleGeofenceTransitionedV1(
                transition.transitionId(), tenantA,
                OffsetDateTime.ofInstant(transition.sourceTimestamp(), java.time.ZoneOffset.UTC),
                transition.geofenceId(), transition.vehicleId(), transition.locationId(),
                transition.geofenceType().name(), transition.transitionType().name(),
                transition.severity().name(), transition.sourceTimestamp().toString(),
                transition.definitionVersion());
        withinTenant(tenantA, () -> notificationBridge.handle(replay));
        evaluate(confirming, now);
        assertThat(count("integration_outbox_event", tenantA)).isOne();
        assertNotification(tenantA, tenantB, transition.transitionId());
    }

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
        return fixture(now, UUID.randomUUID());
    }

    private Fixture fixture(Instant now, UUID tenant) {
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

    private void migrateCleanSchemaWithDispatcherFixtures(UUID tenantA, UUID tenantB) {
        flyway.clean();
        Flyway to78 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("78")).load();
        to78.migrate();
        jdbc.update("""
                INSERT INTO tenant(tenant_id,tenant_code,tenant_name,default_currency,default_time_zone,status,
                 created_at,created_by,updated_at,updated_by,version)
                VALUES(?, 'TENANT-B', 'Tenant B', 'LKR', 'Asia/Colombo', 'ACTIVE', now(), 'test', now(), 'test', 0)
                """, tenantB);
        UUID role = UUID.fromString("49000000-0000-0000-0000-000000000010");
        UUID userA = UUID.fromString("49000000-0000-0000-0000-000000000011");
        UUID userB = UUID.fromString("49000000-0000-0000-0000-000000000012");
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?, 'DISPATCHER', 'test', true)", role);
        insertUser(userA, "dispatcher-a");
        insertUser(userB, "dispatcher-b");
        jdbc.update("INSERT INTO app_user_role(user_id,role_id) VALUES(?,?),(?,?)", userA, role, userB, role);
        insertMembership(UUID.fromString("49000000-0000-0000-0000-000000000021"), tenantA, userA, role);
        insertMembership(UUID.fromString("49000000-0000-0000-0000-000000000022"), tenantB, userB, role);
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("79");
    }

    private void insertUser(UUID userId, String username) {
        jdbc.update("""
                INSERT INTO app_user(id,username,email,password_hash,first_name,last_name,active,created_at,updated_at)
                VALUES(?,?,?,'not-used','Test','Dispatcher',true,now(),now())
                """, userId, username, username + "@example.test");
    }

    private void insertMembership(UUID membershipId, UUID tenantId, UUID userId, UUID roleId) {
        jdbc.update("""
                INSERT INTO tenant_membership(membership_id,tenant_id,user_id,status,created_at,created_by,
                 updated_at,updated_by,version) VALUES(?,?,?,'ACTIVE',now(),'test',now(),'test',0)
                """, membershipId, tenantId, userId);
        jdbc.update("INSERT INTO tenant_membership_role(membership_id,role_id) VALUES(?,?)", membershipId, roleId);
    }

    private void assertNotification(UUID tenantA, UUID tenantB, UUID eventId) {
        assertThat(jdbc.queryForMap("""
                SELECT recipient,channel,status,title,message FROM notification
                WHERE tenant_id=? AND event_id=?
                """, tenantA, eventId))
                .containsEntry("recipient", "dispatcher-a")
                .containsEntry("channel", "IN_APP")
                .containsEntry("status", "SENT")
                .satisfies(row -> assertThat(row.get("title").toString())
                        .doesNotContain("{{").doesNotContainIgnoringCase("latitude", "longitude"))
                .satisfies(row -> assertThat(row.get("message").toString())
                        .doesNotContain("{{").doesNotContainIgnoringCase("latitude", "longitude", "device",
                                "provider", "driver", "customer", "credential"));
        assertThat(count("notification", tenantA)).isOne();
        assertThat(count("notification", tenantB)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule_execution
                WHERE tenant_id=? AND event_id=? AND resolved_recipient='dispatcher-a'
                  AND outcome='ACCEPTED'
                """, Integer.class, tenantA, eventId)).isOne();
    }

    private int count(String table, UUID tenantId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id=?",
                Integer.class, tenantId);
    }

    private void withinTenant(UUID tenantId, Runnable work) {
        tenantContexts.within(new TenantExecutionContext(
                tenantId, ACTOR, "geofence-acceptance", "geofence-acceptance"), work);
    }

    private static String hex(UUID value) {
        return value.toString().replace("-", "") + value.toString().replace("-", "");
    }

    private record Fixture(UUID tenant, UUID device, UUID vehicle, UUID geofenceId) {
    }
}
