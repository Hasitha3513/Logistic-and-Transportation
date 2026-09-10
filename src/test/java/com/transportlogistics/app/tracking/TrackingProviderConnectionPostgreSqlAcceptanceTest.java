package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnectionMutation;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class TrackingProviderConnectionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-09T06:00:00Z");

    @Autowired TrackingProviderConnectionStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void cleanSchemaReachesCurrentHeadAndPreservesTheProviderConnectionAuthority() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("78");
        assertThat(columns("tracking_provider_binding")).contains(
                "provider_type", "display_name", "endpoint_uri", "safe_configuration",
                "poll_interval_seconds", "page_size", "test_status", "last_tested_at",
                "last_successful_poll_at", "last_provider_message_at", "last_error_category",
                "next_poll_at", "lease_owner", "lease_until");
        assertThat(tableExists("tracking_provider_connection")).isFalse();
        assertThat(tableExists("tracking_device_provider_binding")).isTrue();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,created_at,updated_at)
                VALUES(gen_random_uuid(),gen_random_uuid(),'future-device','FIXTURE','UNKNOWN',now(),?,now(),now())
                """, ACTOR)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void realisticV74UpgradeBackfillsVerifiedAliasesWithoutChangingActiveLifecycle() {
        flyway.clean();
        Flyway v74 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("74")).load();
        v74.migrate();
        UUID tenant = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_provider_binding(id,tenant_id,provider_key_id,provider_alias,
                 credential_reference,lifecycle,created_at,created_by,updated_at,updated_by)
                VALUES(?,?,?,'FLESPI','env:UPGRADE_REFERENCE','ACTIVE',now(),?,now(),?)
                """, id, tenant, "upgrade-key", ACTOR, ACTOR);
        long started = System.nanoTime();
        Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders()).load().migrate();
        long milliseconds = (System.nanoTime() - started) / 1_000_000;
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT provider_type,display_name,lifecycle,test_status,safe_configuration::text AS config
                FROM tracking_provider_binding WHERE id=?
                """, id);
        assertThat(row).containsEntry("provider_type", "FLESPI")
                .containsEntry("display_name", "FLESPI")
                .containsEntry("lifecycle", "ACTIVE")
                .containsEntry("test_status", "NOT_TESTED")
                .containsEntry("config", "{}");
        System.out.printf("US48_V74_TO_V75_MIGRATION_MS=%d%n", milliseconds);
    }

    @Test
    void providerConnectionRoundTripPersistsOnlyProviderNeutralSafeFacts() {
        TrackingProviderConnection connection = store.create(newConnection(
                UUID.randomUUID(), "roundtrip", "FLESPI_MAIN", "Main Flespi"));
        Instant tested = NOW.plusSeconds(30);
        TrackingProviderConnection updated = store.update(
                connection.tenantId(), connection.id().value(), connection.version(),
                new TrackingProviderConnectionMutation(
                        "env:ROTATED_REFERENCE", ProviderType.of("FLESPI"), "Primary Flespi",
                        URI.create("https://flespi.example/gw"),
                        new ProviderSafeConfiguration(Map.of("region", "eu")), 30, 250,
                        ProviderConnectionLifecycle.ACTIVE, ProviderConnectionTestStatus.PASS,
                        tested, tested.plusSeconds(1), tested.plusSeconds(2), null,
                        tested.plusSeconds(30), "node-a", tested.plusSeconds(60)), ACTOR, tested);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.credentialReference()).isEqualTo("env:ROTATED_REFERENCE");
        assertThat(updated.safeConfiguration().values()).containsEntry("region", "eu");
        assertThat(updated.testStatus()).isEqualTo(ProviderConnectionTestStatus.PASS);
        assertThat(updated.lastProviderMessageAt()).isEqualTo(tested.plusSeconds(2));
        assertThat(updated.leaseOwner()).isEqualTo("node-a");
        var audit = jdbc.queryForList("""
                SELECT action,safe_detail FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=? ORDER BY occurred_at,action
                """, connection.tenantId(), connection.id().value());
        assertThat(audit).hasSize(2);
        assertThat(audit.toString()).contains("PROVIDER_CONNECTION_CREATED")
                .contains("PROVIDER_CONNECTION_TESTED")
                .doesNotContain("env:ROTATED_REFERENCE");
    }

    @Test
    void everyHumanPersistenceLookupAndMutationIsTenantScoped() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        TrackingProviderConnection connection = store.create(
                newConnection(tenantB, "tenant-b", "FIXTURE_B", "Tenant B"));
        assertThat(store.find(tenantA, connection.id().value())).isEmpty();
        assertThat(store.list(tenantA)).isEmpty();
        assertThatThrownBy(() -> store.update(
                tenantA, connection.id().value(), connection.version(), mutation(connection), ACTOR, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_STALE_VERSION"));
        assertThat(store.find(tenantB, connection.id().value())).contains(connection);
    }

    @Test
    void uniquenessIsTenantLocalExceptForTheOpaqueProviderKey() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        store.create(newConnection(tenantA, "global-key", "SHARED", "Shared name"));
        assertThatThrownBy(() -> store.create(
                newConnection(tenantA, "other-key", "SHARED", "Other name")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> store.create(
                newConnection(tenantA, "third-key", "OTHER", "Shared name")))
                .isInstanceOf(ConflictException.class);
        store.create(newConnection(tenantB, "tenant-b-key", "SHARED", "Shared name"));
        assertThatThrownBy(() -> store.create(
                newConnection(UUID.randomUUID(), "global-key", "UNIQUE", "Unique")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void applicationRejectsSecretConfigurationAndCredentialBearingEndpointsBeforeJdbc() {
        assertThatThrownBy(() -> new ProviderSafeConfiguration(Map.of("authorization", "not-written")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newConnection(
                UUID.randomUUID(), "secret-url", "FLESPI", "Secret URL",
                URI.create("https://provider.example/messages?apiKey=not-written")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_provider_binding",
                Integer.class)).isZero();
    }

    @Test
    void databaseRejectsEveryV75InvalidStateEvenWhenApplicationValidationIsBypassed() {
        TrackingProviderConnection connection = store.create(newConnection(
                UUID.randomUUID(), "constraints", "FIXTURE", "Constraints"));
        UUID id = connection.id().value();
        assertInvalid(id, "provider_type='bad-type'");
        assertInvalid(id, "poll_interval_seconds=4");
        assertInvalid(id, "page_size=501");
        assertInvalid(id, "safe_configuration=jsonb_build_object('safe',repeat('x',8200))");
        assertInvalid(id, "lease_owner='half-lease',lease_until=NULL");
        assertInvalid(id, "lifecycle='UNKNOWN'");
        assertInvalid(id, "test_status='UNKNOWN'");
        assertInvalid(id, "last_error_category='unsafe detail'");
    }

    @Test
    void optimisticVersionPreventsLostUpdateAndRetiredLifecycleIsTerminal() {
        TrackingProviderConnection first = store.create(newConnection(
                UUID.randomUUID(), "version", "FIXTURE", "Versioned"));
        TrackingProviderConnection writerA = store.update(first.tenantId(), first.id().value(), 0,
                mutation(first), ACTOR, NOW.plusSeconds(1));
        assertThat(writerA.version()).isEqualTo(1);
        assertThatThrownBy(() -> store.update(first.tenantId(), first.id().value(), 0,
                mutation(first), ACTOR, NOW.plusSeconds(2)))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_STALE_VERSION"));
        TrackingProviderConnection retired = store.update(first.tenantId(), first.id().value(), 1,
                mutation(writerA, ProviderConnectionLifecycle.RETIRED), ACTOR, NOW.plusSeconds(3));
        assertThatThrownBy(() -> store.update(first.tenantId(), first.id().value(), 2,
                mutation(retired, ProviderConnectionLifecycle.ACTIVE), ACTOR, NOW.plusSeconds(4)))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("TRACKING_PROVIDER_CONNECTION_INVALID"));
    }

    @Test
    void representativePlansUseTheTenantLookupAndFutureDueIndexes() {
        jdbc.update("""
                INSERT INTO tracking_provider_binding(
                 id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                 created_at,created_by,updated_at,updated_by,provider_type,display_name,next_poll_at)
                SELECT gen_random_uuid(),
                 ('00000000-0000-0000-0000-'||lpad((((g-1)%100)+1)::text,12,'0'))::uuid,
                 'scale-'||g,'P_'||g,'env:SCALE_REFERENCE','ACTIVE',now(),?,now(),?,
                 'FIXTURE','Connection '||g,now()-interval '1 minute'
                FROM generate_series(1,10000) g
                """, ACTOR, ACTOR);
        jdbc.execute("ANALYZE tracking_provider_binding");
        String tenant = "00000000-0000-0000-0000-000000000001";
        assertPlanContains("SELECT * FROM tracking_provider_binding WHERE tenant_id='" + tenant
                + "'::uuid ORDER BY display_name", "uq_tracking_provider_display_name");
        assertPlanContains("SELECT * FROM tracking_provider_binding WHERE tenant_id='" + tenant
                + "'::uuid AND provider_alias='P_1'", "uq_tracking_provider_alias");
        assertPlanContains("SELECT * FROM tracking_provider_binding WHERE provider_key_id='scale-1'",
                "uq_tracking_provider_key_id");
        assertPlanContains("SELECT * FROM tracking_provider_binding WHERE lifecycle='ACTIVE' "
                + "AND next_poll_at<=now() ORDER BY next_poll_at LIMIT 100", "idx_tracking_provider_due");
    }

    private NewTrackingProviderConnection newConnection(
            UUID tenant, String key, String alias, String name) {
        return newConnection(tenant, key, alias, name, URI.create("https://provider.example/api"));
    }

    private NewTrackingProviderConnection newConnection(
            UUID tenant, String key, String alias, String name, URI endpoint) {
        return new NewTrackingProviderConnection(
                tenant, ACTOR, key, alias, "env:PROVIDER_REFERENCE", ProviderType.of("FIXTURE"),
                name, endpoint, new ProviderSafeConfiguration(Map.of("region", "test")),
                5, 500, ProviderConnectionLifecycle.ACTIVE, NOW);
    }

    private TrackingProviderConnectionMutation mutation(TrackingProviderConnection connection) {
        return mutation(connection, connection.lifecycle());
    }

    private TrackingProviderConnectionMutation mutation(
            TrackingProviderConnection connection, ProviderConnectionLifecycle lifecycle) {
        return new TrackingProviderConnectionMutation(
                connection.credentialReference(), connection.providerType(), connection.displayName(),
                connection.endpoint(), connection.safeConfiguration(), connection.pollIntervalSeconds(),
                connection.pageSize(), lifecycle, connection.testStatus(), connection.lastTestedAt(),
                connection.lastSuccessfulPollAt(), connection.lastProviderMessageAt(),
                connection.lastErrorCategory(), connection.nextPollAt(), connection.leaseOwner(),
                connection.leaseUntil());
    }

    private void assertInvalid(UUID id, String assignment) {
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE tracking_provider_binding SET " + assignment + " WHERE id=?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertPlanContains(String query, String index) {
        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN (COSTS OFF) " + query, String.class));
        System.out.printf("US48_PLAN_%s=%s%n", index, plan.replace('\n', ' '));
        assertThat(plan).contains(index);
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('public.'||?) IS NOT NULL", Boolean.class, table));
    }

    private java.util.List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name=?
                """, String.class, table);
    }
}
