package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEventPublisherPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RouteDeviationNotificationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final UUID TENANT_B = UUID.fromString("51000000-0000-0000-0000-000000000002");
    private static final UUID ACTOR = UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final Instant SOURCE = Instant.parse("2026-09-14T01:00:00Z");

    @Autowired RouteDeviationEventPublisherPort events;
    @Autowired DurableEventWorker outboxWorker;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    @Test
    void directHighCreatesOneCriticalSameTenantDispatcherNotificationAndReplayIsIdempotent() {
        migrateWithDispatcherFixtures();
        UUID episode = UUID.randomUUID();
        var event = new RouteDeviationEventPublisherPort.Detected(
                episode, UUID.randomUUID(), null, null, UUID.randomUUID(), "REVISION:1", "HIGH",
                new BigDecimal("250.4"), new BigDecimal("100"), SOURCE, true);

        withinTenant(() -> events.publishDetected(TENANT_A, event));
        assertThat(jdbc.queryForMap("""
                SELECT event_type,event_version,consumer_name,aggregate_type,aggregate_id,status,payload::text payload
                FROM integration_outbox_event WHERE tenant_id=? AND event_id=?
                """, TENANT_A, episode))
                .containsEntry("event_type", VehicleRouteDeviationDetectedV1.EVENT_TYPE)
                .containsEntry("event_version", 1)
                .containsEntry("consumer_name", VehicleRouteDeviationDetectedV1.CONSUMER)
                .containsEntry("aggregate_type", "ROUTE_DEVIATION_EPISODE")
                .containsEntry("aggregate_id", episode)
                .containsEntry("status", "PENDING")
                .satisfies(row -> assertThat(row.get("payload").toString())
                        .contains("\"severity\": \"HIGH\"")
                        .doesNotContainIgnoringCase("latitude", "longitude", "geometry", "provider",
                                "device", "credential", "signature", "reviewNotes", "rawTelemetry"));

        withinTenant(outboxWorker::processDue);
        assertThat(jdbc.queryForMap("""
                SELECT recipient,channel,status,severity,title,message FROM notification
                WHERE tenant_id=? AND event_id=?
                """, TENANT_A, episode))
                .containsEntry("recipient", "route-dispatcher-a")
                .containsEntry("channel", "IN_APP")
                .containsEntry("status", "SENT")
                .containsEntry("severity", "CRITICAL")
                .satisfies(row -> assertThat(row.get("title").toString()).endsWith("HIGH"))
                .satisfies(row -> assertThat(row.get("message").toString())
                        .contains("250 m", "Approval is required.")
                        .doesNotContainIgnoringCase("driver", "latitude", "longitude", "provider",
                                "device", "credential", "signature"));
        assertThat(count("notification", TENANT_B)).isZero();

        withinTenant(() -> events.publishDetected(TENANT_A, event));
        withinTenant(outboxWorker::processDue);
        assertThat(count("integration_outbox_event", TENANT_A)).isOne();
        assertThat(count("notification", TENANT_A)).isOne();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule_execution
                WHERE tenant_id=? AND event_id=? AND resolved_recipient='route-dispatcher-a'
                  AND outcome='ACCEPTED'
                """, Integer.class, TENANT_A, episode)).isOne();
    }

    private void migrateWithDispatcherFixtures() {
        flyway.clean();
        Flyway to89 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("89")).load();
        to89.migrate();
        jdbc.update("""
                INSERT INTO tenant(tenant_id,tenant_code,tenant_name,default_currency,default_time_zone,status,
                 created_at,created_by,updated_at,updated_by,version)
                VALUES(?, 'ROUTE-B', 'Route Tenant B', 'LKR', 'Asia/Colombo', 'ACTIVE', now(), 'test', now(), 'test', 0)
                """, TENANT_B);
        UUID role = UUID.fromString("51000000-0000-0000-0000-000000000010");
        UUID userA = UUID.fromString("51000000-0000-0000-0000-000000000011");
        UUID userB = UUID.fromString("51000000-0000-0000-0000-000000000012");
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?, 'DISPATCHER', 'test', true)", role);
        insertUser(userA, "route-dispatcher-a");
        insertUser(userB, "route-dispatcher-b");
        jdbc.update("INSERT INTO app_user_role(user_id,role_id) VALUES(?,?),(?,?)", userA, role, userB, role);
        insertMembership(UUID.fromString("51000000-0000-0000-0000-000000000021"), TENANT_A, userA, role);
        insertMembership(UUID.fromString("51000000-0000-0000-0000-000000000022"), TENANT_B, userB, role);
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("102");
    }

    private void insertUser(UUID id, String username) {
        jdbc.update("""
                INSERT INTO app_user(id,username,email,password_hash,first_name,last_name,active,created_at,updated_at)
                VALUES(?,?,?,'not-used','Route','Dispatcher',true,now(),now())
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

    private void withinTenant(Runnable work) {
        tenantContexts.within(new TenantExecutionContext(
                TENANT_A, ACTOR, "route-notification-acceptance", "route-notification-acceptance"), work);
    }
}
