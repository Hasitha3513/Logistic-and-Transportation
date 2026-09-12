package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.system.infrastructure.adapters.in.events.SpeedingEpisodeNotificationBridge;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.application.SpeedEvaluationService;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SpeedNotificationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final Instant BASE = Instant.parse("2026-09-12T04:00:00Z");
    @Autowired SpeedEvaluationService evaluator;
    @Autowired SpeedRuleRepositoryPort rules;
    @Autowired DurableEventWorker outboxWorker;
    @Autowired SpeedingEpisodeNotificationBridge bridge;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;
    @Autowired ObjectMapper objectMapper;

    @Test
    void cleanV1ToV83ProducesTenantSafeWarningAndHighNotificationsWithoutPacketFlood() throws Exception {
        UUID tenantA = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
        UUID tenantB = UUID.fromString("50000000-0000-0000-0000-000000000002");
        migrateWithDispatcherFixtures(tenantA, tenantB);
        UUID vehicle = UUID.randomUUID();
        SpeedRule rule = new SpeedRule(UUID.randomUUID(), tenantA, "Tenant threshold",
                SpeedRule.Scope.TENANT, null, null, new SpeedKph(new BigDecimal("60")),
                SpeedRule.Lifecycle.ACTIVE, 1, BASE);
        withinTenant(tenantA, () -> rules.save(rule, 0));

        evaluate(tenantA, vehicle, 1, "70", BASE.plusSeconds(1));
        var warning = evaluate(tenantA, vehicle, 2, "75", BASE.plusSeconds(2));
        evaluate(tenantA, vehicle, 3, "80", BASE.plusSeconds(3));
        evaluate(tenantA, vehicle, 4, "82", BASE.plusSeconds(4));
        evaluate(tenantA, vehicle, 5, "60", BASE.plusSeconds(5));
        UUID warningId = warning.episode().orElseThrow().id();
        assertOutbox(warningId, tenantA, "WARNING");
        withinTenant(tenantA, outboxWorker::processDue);
        assertNotification(warningId, tenantA, tenantB);

        var high = evaluate(tenantA, vehicle, 6, "70", BASE.plusSeconds(6));
        assertThat(high.episode()).isEmpty();
        var confirmedHigh = evaluate(tenantA, vehicle, 7, "76", BASE.plusSeconds(7));
        UUID highId = confirmedHigh.episode().orElseThrow().id();
        assertOutbox(highId, tenantA, "HIGH");
        withinTenant(tenantA, outboxWorker::processDue);
        assertNotification(highId, tenantA, tenantB);

        withinTenant(tenantA, outboxWorker::processDue);
        VehicleSpeedingDetectedV1 replay = durableEvent(highId);
        withinTenant(tenantA, () -> bridge.handle(replay));
        assertThat(count("integration_outbox_event", tenantA)).isEqualTo(2);
        assertThat(count("notification", tenantA)).isEqualTo(2);
        assertThat(count("notification", tenantB)).isZero();
    }

    private com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult evaluate(
            UUID tenant, UUID vehicle, int sequence, String speed, Instant source) {
        SpeedPosition position = new SpeedPosition(tenant, vehicle, new UUID(0, sequence), source,
                new SpeedKph(new BigDecimal(speed)), false, true, true, true);
        return tenantContexts.within(new TenantExecutionContext(
                tenant, ACTOR, "speed-notification-acceptance", "speed-notification-acceptance"),
                () -> evaluator.evaluate(position, source.plusSeconds(1)));
    }

    private void assertOutbox(UUID eventId, UUID tenant, String severity) {
        var row = jdbc.queryForMap("""
                SELECT event_type,event_version,consumer_name,aggregate_type,aggregate_id,
                       occurred_at,payload::text payload,status
                FROM integration_outbox_event WHERE tenant_id=? AND event_id=?
                """, tenant, eventId);
        assertThat(row).containsEntry("event_type", VehicleSpeedingDetectedV1.EVENT_TYPE)
                .containsEntry("event_version", 1)
                .containsEntry("consumer_name", VehicleSpeedingDetectedV1.CONSUMER)
                .containsEntry("aggregate_type", "SPEEDING_EPISODE")
                .containsEntry("aggregate_id", eventId).containsEntry("status", "PENDING");
        assertThat(jdbc.queryForList("""
                SELECT jsonb_object_keys(payload) FROM integration_outbox_event
                WHERE tenant_id=? AND event_id=? ORDER BY 1
                """, String.class, tenant, eventId)).containsExactly(
                "driverId", "effectiveThresholdKph", "observedSpeedKph", "repeatCount",
                "routeId", "routeVersion", "ruleId", "ruleVersion", "severity",
                "sourceTimestamp", "speedEpisodeId", "thresholdSource", "tripId", "vehicleId");
        assertThat(row.get("payload").toString()).contains("\"severity\": \"" + severity + "\"")
                .doesNotContainIgnoringCase("latitude", "longitude", "positionId", "device",
                        "provider", "imei", "customer", "credential", "rawTelemetry");
    }

    private void assertNotification(UUID eventId, UUID tenantA, UUID tenantB) {
        assertThat(jdbc.queryForMap("""
                SELECT recipient,channel,status,title,message FROM notification
                WHERE tenant_id=? AND event_id=?
                """, tenantA, eventId)).containsEntry("recipient", "speed-dispatcher-a")
                .containsEntry("channel", "IN_APP").containsEntry("status", "SENT")
                .satisfies(row -> assertThat(row.get("message").toString())
                        .contains("configured speed threshold")
                        .doesNotContainIgnoringCase("legal speed limit", "traffic offence", "latitude",
                                "longitude", "device", "provider", "driver", "customer", "credential"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule_execution
                WHERE tenant_id=? AND event_id=? AND resolved_recipient='speed-dispatcher-a'
                  AND outcome='ACCEPTED'
                """, Integer.class, tenantA, eventId)).isOne();
        assertThat(count("notification", tenantB)).isZero();
    }

    private VehicleSpeedingDetectedV1 durableEvent(UUID eventId) throws Exception {
        var row = jdbc.queryForMap("""
                SELECT tenant_id,occurred_at,payload FROM integration_outbox_event WHERE event_id=?
                """, eventId);
        var payload = objectMapper.readValue(row.get("payload").toString(),
                new TypeReference<java.util.Map<String, Object>>() { });
        Object occurredAt = row.get("occurred_at");
        java.time.OffsetDateTime offset = occurredAt instanceof java.time.OffsetDateTime value ? value
                : java.time.OffsetDateTime.ofInstant(((java.sql.Timestamp) occurredAt).toInstant(),
                        java.time.ZoneOffset.UTC);
        return new VehicleSpeedingDetectedV1(eventId, (UUID) row.get("tenant_id"), offset,
                UUID.fromString(payload.get("vehicleId").toString()), null, null, null, null,
                new BigDecimal(payload.get("observedSpeedKph").toString()),
                new BigDecimal(payload.get("effectiveThresholdKph").toString()),
                payload.get("thresholdSource").toString(), UUID.fromString(payload.get("ruleId").toString()),
                Long.parseLong(payload.get("ruleVersion").toString()), payload.get("severity").toString(),
                payload.get("sourceTimestamp").toString(), Integer.parseInt(payload.get("repeatCount").toString()));
    }

    private void migrateWithDispatcherFixtures(UUID tenantA, UUID tenantB) {
        flyway.clean();
        Flyway to82 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("82")).load();
        to82.migrate();
        jdbc.update("""
                INSERT INTO tenant(tenant_id,tenant_code,tenant_name,default_currency,default_time_zone,status,
                 created_at,created_by,updated_at,updated_by,version)
                VALUES(?, 'SPEED-B', 'Speed Tenant B', 'LKR', 'Asia/Colombo', 'ACTIVE', now(), 'test', now(), 'test', 0)
                """, tenantB);
        UUID role = UUID.fromString("50000000-0000-0000-0000-000000000010");
        UUID userA = UUID.fromString("50000000-0000-0000-0000-000000000011");
        UUID userB = UUID.fromString("50000000-0000-0000-0000-000000000012");
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?, 'DISPATCHER', 'test', true)", role);
        insertUser(userA, "speed-dispatcher-a");
        insertUser(userB, "speed-dispatcher-b");
        jdbc.update("INSERT INTO app_user_role(user_id,role_id) VALUES(?,?),(?,?)", userA, role, userB, role);
        insertMembership(UUID.fromString("50000000-0000-0000-0000-000000000021"), tenantA, userA, role);
        insertMembership(UUID.fromString("50000000-0000-0000-0000-000000000022"), tenantB, userB, role);
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
    }

    private void insertUser(UUID id, String username) {
        jdbc.update("""
                INSERT INTO app_user(id,username,email,password_hash,first_name,last_name,active,created_at,updated_at)
                VALUES(?,?,?,'not-used','Speed','Dispatcher',true,now(),now())
                """, id, username, username + "@example.test");
    }

    private void insertMembership(UUID id, UUID tenant, UUID user, UUID role) {
        jdbc.update("""
                INSERT INTO tenant_membership(membership_id,tenant_id,user_id,status,created_at,created_by,
                 updated_at,updated_by,version) VALUES(?,?,?,'ACTIVE',now(),'test',now(),'test',0)
                """, id, tenant, user);
        jdbc.update("INSERT INTO tenant_membership_role(membership_id,role_id) VALUES(?,?)", id, role);
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id=?",
                Integer.class, tenant);
    }

    private void withinTenant(UUID tenant, Runnable work) {
        tenantContexts.within(new TenantExecutionContext(
                tenant, ACTOR, "speed-notification-acceptance", "speed-notification-acceptance"), work);
    }
}
