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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
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

    @Test
    void concurrentDuplicateFirstObservationCreatesOneSilentStableState() throws Exception {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        UUID initial = position(fixture, now.minusSeconds(1), 20, 20);

        runConcurrently(() -> evaluate(initial, now), () -> evaluate(initial, now));

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_vehicle_geofence_state
                WHERE tenant_id=? AND geofence_id=? AND vehicle_id=?
                """, Integer.class, fixture.tenant(), fixture.geofenceId(), fixture.vehicle()))
                .isOne();
        assertThat(count("tracking_geofence_transition", fixture.tenant())).isZero();
        assertThat(count("integration_outbox_event", fixture.tenant())).isZero();
    }

    @Test
    void concurrentDuplicateConfirmationCreatesOneTransitionAndOneDurableEvent() throws Exception {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        evaluate(position(fixture, now.minusSeconds(3), 20, 20), now);
        evaluate(position(fixture, now.minusSeconds(2), 2, 2), now);
        UUID confirming = position(fixture, now.minusSeconds(1), 3, 2);

        runConcurrently(() -> evaluate(confirming, now), () -> evaluate(confirming, now));

        assertThat(count("tracking_geofence_transition", fixture.tenant())).isOne();
        assertThat(count("integration_outbox_event", fixture.tenant())).isOne();
        assertThat(states.find(fixture.tenant(), fixture.vehicle(), fixture.geofenceId(), 0, 10))
                .singleElement().satisfies(state -> {
                    assertThat(state.stableState().name()).isEqualTo("INSIDE");
                    assertThat(state.lastEvaluatedPositionId()).isEqualTo(confirming);
                });
    }

    @Test
    void concurrentDelayedAndNewerPositionsCannotRewindSourceOrder() throws Exception {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        evaluate(position(fixture, now.minusSeconds(4), 20, 20), now);
        UUID older = position(fixture, now.minusSeconds(2), 2, 2);
        UUID newer = position(fixture, now.minusSeconds(1), 3, 2);

        runConcurrently(() -> evaluate(older, now), () -> evaluate(newer, now));

        assertThat(states.find(fixture.tenant(), fixture.vehicle(), fixture.geofenceId(), 0, 10))
                .singleElement().satisfies(state -> {
                    assertThat(state.lastEvaluatedPositionId()).isEqualTo(newer);
                    assertThat(state.lastEvaluatedSourceTimestamp())
                            .isEqualTo(jdbc.queryForObject(
                                    "SELECT source_timestamp FROM tracking_position WHERE id=?",
                                    Timestamp.class, newer).toInstant());
                });
        assertThat(count("tracking_geofence_transition", fixture.tenant())).isLessThanOrEqualTo(1);
    }

    @Test
    void fiveHundredActiveDefinitionsRemainHardBoundedWithoutPerPacketEvents() {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        jdbc.update("""
                INSERT INTO tracking_geofence(
                 id,tenant_id,name,type,polygon_vertices,min_longitude,max_longitude,min_latitude,
                 max_latitude,location_id,alert_enter_enabled,alert_exit_enabled,lifecycle,version,
                 created_at,created_by,updated_at,updated_by)
                SELECT gen_random_uuid(), ?, 'Scale ' || value, 'UNAUTHORIZED_ZONE',
                 '[{"longitude":79.8,"latitude":6.8},{"longitude":79.9,"latitude":6.8},
                   {"longitude":79.9,"latitude":6.9},{"longitude":79.8,"latitude":6.8}]'::jsonb,
                 79.8,79.9,6.8,6.9,NULL,TRUE,FALSE,'ACTIVE',1,now(),?,now(),?
                FROM generate_series(1,499) value
                """, fixture.tenant(), ACTOR, ACTOR);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_geofence WHERE tenant_id=? AND lifecycle='ACTIVE'
                """, Integer.class, fixture.tenant())).isEqualTo(500);
        UUID first = position(fixture, now.minusSeconds(2), 79.85, 6.85);
        long started = System.nanoTime();
        evaluate(first, now);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_vehicle_geofence_state
                WHERE tenant_id=? AND vehicle_id=?
                """, Integer.class, fixture.tenant(), fixture.vehicle())).isEqualTo(500);
        assertThat(count("tracking_geofence_transition", fixture.tenant())).isZero();
        assertThat(count("integration_outbox_event", fixture.tenant())).isZero();
        System.out.printf("US49_GEOFENCE_500_INITIALIZATION elapsedMs=%d states=500 events=0%n",
                elapsedMillis);
    }

    @Test
    void disableSerializationHonorsBothCommitOrders() {
        Instant now = Instant.now();
        Fixture disableFirst = fixture(now);
        evaluate(position(disableFirst, now.minusSeconds(3), 20, 20), now);
        evaluate(position(disableFirst, now.minusSeconds(2), 2, 2), now);
        Geofence first = geofences.find(disableFirst.tenant(), disableFirst.geofenceId()).orElseThrow();
        long firstVersion = first.version();
        first.disable(now.minusMillis(500), ACTOR);
        geofences.save(first, firstVersion);
        evaluate(position(disableFirst, now.minusSeconds(1), 3, 2), now);
        assertThat(count("tracking_geofence_transition", disableFirst.tenant())).isZero();

        Fixture evaluationFirst = fixture(now);
        evaluate(position(evaluationFirst, now.minusSeconds(3), 20, 20), now);
        evaluate(position(evaluationFirst, now.minusSeconds(2), 2, 2), now);
        evaluate(position(evaluationFirst, now.minusSeconds(1), 3, 2), now);
        Geofence second = geofences.find(
                evaluationFirst.tenant(), evaluationFirst.geofenceId()).orElseThrow();
        long secondVersion = second.version();
        second.disable(now, ACTOR);
        geofences.save(second, secondVersion);
        assertThat(count("tracking_geofence_transition", evaluationFirst.tenant())).isOne();
    }

    @Test
    void retirementSerializationHonorsBothCommitOrdersAndRemainsTerminal() {
        Instant now = Instant.now();
        Fixture retireFirst = fixture(now);
        evaluate(position(retireFirst, now.minusSeconds(3), 20, 20), now);
        evaluate(position(retireFirst, now.minusSeconds(2), 2, 2), now);
        disableAndRetire(retireFirst, now);
        evaluate(position(retireFirst, now.minusSeconds(1), 3, 2), now);
        assertThat(count("tracking_geofence_transition", retireFirst.tenant())).isZero();

        Fixture evaluationFirst = fixture(now);
        evaluate(position(evaluationFirst, now.minusSeconds(3), 20, 20), now);
        evaluate(position(evaluationFirst, now.minusSeconds(2), 2, 2), now);
        evaluate(position(evaluationFirst, now.minusSeconds(1), 3, 2), now);
        disableAndRetire(evaluationFirst, now);
        assertThat(count("tracking_geofence_transition", evaluationFirst.tenant())).isOne();
        assertThat(geofences.find(evaluationFirst.tenant(), evaluationFirst.geofenceId())
                .orElseThrow().lifecycle().name()).isEqualTo("RETIRED");
    }

    @Test
    void reactivatedDefinitionVersionSilentlyReinitializesOldState() {
        Instant now = Instant.now();
        Fixture fixture = fixture(now);
        evaluate(position(fixture, now.minusSeconds(3), 20, 20), now);
        Geofence definition = geofences.find(fixture.tenant(), fixture.geofenceId()).orElseThrow();
        long activeVersion = definition.version();
        definition.disable(now.minusSeconds(2), ACTOR);
        geofences.save(definition, activeVersion);
        long disabledVersion = definition.version();
        definition.updateDefinition("Unauthorized revised", GeofenceType.UNAUTHORIZED_ZONE,
                definition.polygon(), null, GeofenceAlertPolicy.unauthorizedZone(),
                now.minusSeconds(1), ACTOR);
        geofences.save(definition, disabledVersion);
        long revisedVersion = definition.version();
        definition.activate(now.minusMillis(500), ACTOR);
        geofences.save(definition, revisedVersion);

        evaluate(position(fixture, now.minusMillis(100), 2, 2), now);
        assertThat(count("tracking_geofence_transition", fixture.tenant())).isZero();
        assertThat(states.find(fixture.tenant(), fixture.vehicle(), fixture.geofenceId(), 0, 10))
                .singleElement().satisfies(state -> {
                    assertThat(state.definitionVersion()).isEqualTo(definition.version());
                    assertThat(state.stableState().name()).isEqualTo("INSIDE");
                    assertThat(state.pendingCount()).isZero();
                });
    }

    private void disableAndRetire(Fixture fixture, Instant now) {
        Geofence definition = geofences.find(fixture.tenant(), fixture.geofenceId()).orElseThrow();
        long activeVersion = definition.version();
        definition.disable(now, ACTOR);
        geofences.save(definition, activeVersion);
        long disabledVersion = definition.version();
        definition.retire(now.plusMillis(1), ACTOR);
        geofences.save(definition, disabledVersion);
    }

    private void runConcurrently(Runnable firstWork, Runnable secondWork) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await();
                firstWork.run();
                return true;
            });
            var second = executor.submit(() -> {
                barrier.await();
                secondWork.run();
                return true;
            });
            assertThat(first.get()).isTrue();
            assertThat(second.get()).isTrue();
        }
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
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
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
