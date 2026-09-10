package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class TrackingDeviceProviderBindingPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-09T09:00:00Z");

    @Autowired TrackingDeviceProviderBindingStore store;
    @Autowired TrackingProviderConnectionStore connections;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void cleanSchemaReachesCurrentHeadWithTheAuthorizedBindingTable() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("79");
        assertThat(tableExists("tracking_device_provider_binding")).isTrue();
        assertThat(tableExists("tracking_provider_connection")).isFalse();
        assertThat(columns()).containsExactlyInAnyOrder(
                "id", "tenant_id", "tracking_device_id", "provider_binding_id",
                "external_device_reference", "safe_configuration", "lifecycle",
                "watermark_source_timestamp", "watermark_message_identity", "next_poll_at",
                "created_at", "updated_at", "created_by", "updated_by", "version");
        assertThat(columns("tracking_device")).contains("provider_alias", "external_device_reference");
    }

    @Test
    void realisticV75UpgradeBackfillsExactlyOneSameTenantBinding() {
        migrateTo75();
        UUID tenant = UUID.randomUUID();
        UUID provider = insertProvider(tenant, "FLESPI", "ACTIVE");
        UUID active = insertDevice(tenant, "FLESPI", "device-active", "ACTIVE");
        UUID disabled = insertDevice(tenant, "FLESPI", "device-disabled", "DISABLED");
        long started = System.nanoTime();
        migrateCurrent();
        long milliseconds = (System.nanoTime() - started) / 1_000_000;
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_device_provider_binding
                WHERE tenant_id=? AND provider_binding_id=?
                """, Integer.class, tenant, provider)).isEqualTo(2);
        assertThat(lifecycle(active)).isEqualTo("ACTIVE");
        assertThat(lifecycle(disabled)).isEqualTo("DISABLED");
        assertThat(jdbc.queryForMap("""
                SELECT safe_configuration::text config,watermark_source_timestamp,
                       watermark_message_identity,next_poll_at,version
                FROM tracking_device_provider_binding WHERE tracking_device_id=?
                """, active)).containsEntry("config", "{}").containsEntry("version", 0L)
                .containsEntry("watermark_source_timestamp", null)
                .containsEntry("watermark_message_identity", null).containsEntry("next_poll_at", null);
        System.out.printf("US48_V75_TO_V76_MIGRATION_MS=%d%n", milliseconds);
    }

    @Test
    void backfillFailsClosedForZeroAmbiguousAndCrossTenantMatches() {
        migrateTo75();
        UUID tenant = UUID.randomUUID();
        insertDevice(tenant, "MISSING", "unmatched", "ACTIVE");
        assertThatThrownBy(this::migrateCurrent).isInstanceOf(FlywayException.class)
                .hasMessageContaining("not deterministic");

        migrateTo75();
        tenant = UUID.randomUUID();
        insertProvider(tenant, "FIXTURE", "ACTIVE");
        jdbc.execute("ALTER TABLE tracking_provider_binding DROP CONSTRAINT uq_tracking_provider_alias");
        insertProvider(tenant, "FIXTURE", "ACTIVE");
        insertDevice(tenant, "FIXTURE", "ambiguous", "ACTIVE");
        assertThatThrownBy(this::migrateCurrent).isInstanceOf(FlywayException.class)
                .hasMessageContaining("not deterministic");

        migrateTo75();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        insertProvider(tenantB, "FLESPI", "ACTIVE");
        insertDevice(tenantA, "FLESPI", "cross-tenant", "ACTIVE");
        assertThatThrownBy(this::migrateCurrent).isInstanceOf(FlywayException.class)
                .hasMessageContaining("not deterministic");
    }

    @Test
    void roundTripTenantReadsWatermarkNextPollAndCompatibilityProjection() {
        Fixture fixture = fixture("roundtrip");
        TrackingDeviceProviderBinding binding = store.create(command(
                fixture, "provider-device-1", DeviceProviderBindingLifecycle.ACTIVE));
        assertThat(store.find(fixture.tenant(), binding.id())).contains(binding);
        assertThat(store.find(UUID.randomUUID(), binding.id())).isEmpty();
        assertThat(store.findActiveByDevice(fixture.tenant(), fixture.device())).contains(binding);
        assertThat(store.findByExternalReference(
                fixture.tenant(), fixture.connection(), "provider-device-1")).contains(binding);
        assertThat(store.listByProviderConnection(fixture.tenant(), fixture.connection(), 10))
                .containsExactly(binding);
        assertThat(jdbc.queryForMap("""
                SELECT action,safe_detail FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=?
                """, fixture.tenant(), binding.id()))
                .containsEntry("action", "DEVICE_PROVIDER_BOUND")
                .containsEntry("safe_detail", "LIFECYCLE=ACTIVE")
                .doesNotContainValue("provider-device-1");
        Instant source = NOW.plusSeconds(10);
        Instant poll = NOW.plusSeconds(30);
        TrackingDeviceProviderBinding updated = store.updateWatermark(
                fixture.tenant(), binding.id(), 0, source, "message-1", poll, ACTOR, NOW.plusSeconds(1));
        assertThat(updated.watermarkSourceTimestamp()).isEqualTo(source);
        assertThat(updated.watermarkMessageIdentity()).isEqualTo("message-1");
        assertThat(updated.nextPollAt()).isEqualTo(poll);
        TrackingDeviceProviderBinding rescheduled = store.updateNextPoll(
                fixture.tenant(), binding.id(), 1, poll.plusSeconds(30), ACTOR, NOW.plusSeconds(2));
        assertThat(rescheduled.nextPollAt()).isEqualTo(poll.plusSeconds(30));
        assertThat(rescheduled.version()).isEqualTo(2);
        assertThat(jdbc.queryForMap("""
                SELECT provider_alias,external_device_reference FROM tracking_device
                WHERE tenant_id=? AND id=?
                """, fixture.tenant(), fixture.device()))
                .containsEntry("provider_alias", fixture.alias())
                .containsEntry("external_device_reference", "provider-device-1");
    }

    @Test
    void activationRequiresSameTenantActiveDeviceAndProvider() {
        Fixture fixture = fixture("preconditions");
        TrackingDeviceProviderBinding draft = store.create(command(
                fixture, "draft-device", DeviceProviderBindingLifecycle.DRAFT));
        jdbc.update("UPDATE tracking_device SET lifecycle='DISABLED' WHERE id=?", fixture.device());
        assertInvalidActivation(fixture.tenant(), draft);
        jdbc.update("UPDATE tracking_device SET lifecycle='ACTIVE' WHERE id=?", fixture.device());
        jdbc.update("UPDATE tracking_provider_binding SET lifecycle='DISABLED' WHERE id=?",
                fixture.connection().value());
        assertInvalidActivation(fixture.tenant(), draft);
        jdbc.update("UPDATE tracking_provider_binding SET lifecycle='ACTIVE' WHERE id=?",
                fixture.connection().value());
        TrackingDeviceProviderBinding active = store.updateLifecycle(
                fixture.tenant(), draft.id(), draft.version(), DeviceProviderBindingLifecycle.ACTIVE,
                ACTOR, NOW.plusSeconds(3));
        assertThat(active.lifecycle()).isEqualTo(DeviceProviderBindingLifecycle.ACTIVE);

        Fixture other = fixture("other-tenant");
        assertThatThrownBy(() -> store.create(new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.device(), other.connection(), "cross-tenant",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                null, ACTOR, NOW))).isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_DEVICE_PROVIDER_INVALID"));
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tracking_device_provider_binding(
                 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
                 lifecycle,created_at,updated_at,created_by,updated_by)
                VALUES(?,?,?,?,?,'DRAFT',now(),now(),?,?)
                """, UUID.randomUUID(), fixture.tenant(), fixture.device(),
                other.connection().value(), "cross-tenant-db", ACTOR, ACTOR))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniquenessScopeLifecycleAndSafeConfigurationAreEnforced() {
        Fixture first = fixture("constraints");
        TrackingDeviceProviderBinding binding = store.create(command(
                first, "same-reference", DeviceProviderBindingLifecycle.ACTIVE));
        UUID secondDevice = insertDevice(first.tenant(), first.alias(), "legacy-second", "ACTIVE");
        assertThatThrownBy(() -> store.create(new NewTrackingDeviceProviderBinding(
                first.tenant(), secondDevice, first.connection(), "same-reference",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                null, ACTOR, NOW))).isInstanceOf(BusinessRuleException.class);
        ProviderConnectionId secondConnection = connection(first.tenant(), "SECOND");
        store.create(new NewTrackingDeviceProviderBinding(
                first.tenant(), secondDevice, secondConnection, "same-reference",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                null, ACTOR, NOW));
        Fixture otherTenant = fixture("scope");
        store.create(command(otherTenant, "same-reference", DeviceProviderBindingLifecycle.ACTIVE));
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE tracking_device_provider_binding SET lifecycle='UNKNOWN' WHERE id=?", binding.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE tracking_device_provider_binding
                SET safe_configuration=jsonb_build_object('safe',repeat('x',4200)) WHERE id=?
                """, binding.id())).isInstanceOf(DataIntegrityViolationException.class);
        Map<String, String> large = new LinkedHashMap<>();
        for (int index = 0; index < 10; index++) {
            large.put("safe" + index, "x".repeat(500));
        }
        assertThatThrownBy(() -> store.create(new NewTrackingDeviceProviderBinding(
                first.tenant(), secondDevice, secondConnection, "large-config",
                new ProviderSafeConfiguration(large), DeviceProviderBindingLifecycle.DRAFT,
                null, ACTOR, NOW))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4096");
        assertThatThrownBy(() -> new ProviderSafeConfiguration(Map.of("private_key", "not-written")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rebindIsAtomicPreservesHistoryAndUpdatesCompatibility() {
        Fixture fixture = fixture("rebind");
        TrackingDeviceProviderBinding original = store.create(command(
                fixture, "old-reference", DeviceProviderBindingLifecycle.ACTIVE));
        ProviderConnectionId replacementConnection = connection(fixture.tenant(), "REPLACEMENT");
        NewTrackingDeviceProviderBinding replacement = new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.device(), replacementConnection, "new-reference",
                new ProviderSafeConfiguration(Map.of("channel", "primary")),
                DeviceProviderBindingLifecycle.ACTIVE, NOW.plusSeconds(30), ACTOR, NOW.plusSeconds(1));
        TrackingDeviceProviderBinding rebound = store.rebind(replacement, original.version());
        assertThat(store.find(fixture.tenant(), original.id()).orElseThrow().lifecycle())
                .isEqualTo(DeviceProviderBindingLifecycle.DISABLED);
        assertThat(store.findActiveByDevice(fixture.tenant(), fixture.device())).contains(rebound);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_device_provider_binding
                WHERE tenant_id=? AND tracking_device_id=?
                """, Integer.class, fixture.tenant(), fixture.device())).isEqualTo(2);
        assertThat(jdbc.queryForMap("""
                SELECT provider_alias,external_device_reference FROM tracking_device WHERE id=?
                """, fixture.device())).containsEntry("provider_alias", "REPLACEMENT")
                .containsEntry("external_device_reference", "new-reference");
    }

    @Test
    void retiredIsTerminalAndStaleVersionsCannotMutate() {
        Fixture fixture = fixture("retired");
        TrackingDeviceProviderBinding draft = store.create(command(
                fixture, "retired-reference", DeviceProviderBindingLifecycle.DRAFT));
        TrackingDeviceProviderBinding retired = store.updateLifecycle(
                fixture.tenant(), draft.id(), 0, DeviceProviderBindingLifecycle.RETIRED,
                ACTOR, NOW.plusSeconds(1));
        assertThatThrownBy(() -> store.updateLifecycle(
                fixture.tenant(), retired.id(), 1, DeviceProviderBindingLifecycle.ACTIVE,
                ACTOR, NOW.plusSeconds(2))).isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_DEVICE_PROVIDER_INVALID"));
        assertThatThrownBy(() -> store.updateNextPoll(
                fixture.tenant(), retired.id(), 0, NOW, ACTOR, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_STALE_VERSION"));
        assertThatThrownBy(() -> store.updateNextPoll(
                UUID.randomUUID(), retired.id(), 1, NOW, ACTOR, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_STALE_VERSION"));
    }

    @Test
    void fourBindingRacesHaveOneWinnerAndNeverLeaveDoubleActiveState() throws Exception {
        Fixture activeFixture = fixture("race-active");
        TrackingDeviceProviderBinding firstDraft = store.create(command(
                activeFixture, "race-first", DeviceProviderBindingLifecycle.DRAFT));
        ProviderConnectionId otherConnection = connection(activeFixture.tenant(), "RACE_OTHER");
        TrackingDeviceProviderBinding secondDraft = store.create(new NewTrackingDeviceProviderBinding(
                activeFixture.tenant(), activeFixture.device(), otherConnection, "race-second",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                null, ACTOR, NOW));
        List<Object> doubleActive = race(
                () -> activate(activeFixture.tenant(), firstDraft),
                () -> activate(activeFixture.tenant(), secondDraft));
        assertOneSuccess(doubleActive);
        assertActiveCount(activeFixture, 1);

        Fixture duplicate = fixture("race-duplicate");
        UUID otherDevice = insertDevice(duplicate.tenant(), duplicate.alias(), "race-device-2", "ACTIVE");
        List<Object> duplicateReference = race(
                () -> store.create(command(duplicate, "duplicate-ref", DeviceProviderBindingLifecycle.DRAFT)),
                () -> store.create(new NewTrackingDeviceProviderBinding(
                        duplicate.tenant(), otherDevice, duplicate.connection(), "duplicate-ref",
                        ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                        null, ACTOR, NOW)));
        assertOneSuccess(duplicateReference);

        Fixture rebindFixture = fixture("race-rebind");
        TrackingDeviceProviderBinding current = store.create(command(
                rebindFixture, "current-ref", DeviceProviderBindingLifecycle.ACTIVE));
        ProviderConnectionId nextConnection = connection(rebindFixture.tenant(), "RACE_REBIND");
        NewTrackingDeviceProviderBinding replacement = new NewTrackingDeviceProviderBinding(
                rebindFixture.tenant(), rebindFixture.device(), nextConnection, "replacement-ref",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                null, ACTOR, NOW);
        List<Object> rebindDisable = race(
                () -> store.rebind(replacement, current.version()),
                () -> store.updateLifecycle(rebindFixture.tenant(), current.id(), current.version(),
                        DeviceProviderBindingLifecycle.DISABLED, ACTOR, NOW));
        assertOneSuccess(rebindDisable);
        assertActiveCount(rebindFixture, jdbc.queryForObject("""
                SELECT count(*) FROM tracking_device_provider_binding
                WHERE tenant_id=? AND tracking_device_id=? AND lifecycle='ACTIVE'
                """, Integer.class, rebindFixture.tenant(), rebindFixture.device()));

        Fixture watermarkFixture = fixture("race-watermark");
        TrackingDeviceProviderBinding watermark = store.create(command(
                watermarkFixture, "watermark-ref", DeviceProviderBindingLifecycle.ACTIVE));
        List<Object> watermarkRace = race(
                () -> store.updateWatermark(watermarkFixture.tenant(), watermark.id(), 0,
                        NOW.plusSeconds(2), "newer", null, ACTOR, NOW.plusSeconds(2)),
                () -> store.updateWatermark(watermarkFixture.tenant(), watermark.id(), 0,
                        NOW.plusSeconds(1), "older", null, ACTOR, NOW.plusSeconds(1)));
        assertOneSuccess(watermarkRace);
        assertThat(store.find(watermarkFixture.tenant(), watermark.id()).orElseThrow().version())
                .isEqualTo(1);
        System.out.println("US48_DEVICE_PROVIDER_BINDING_RACES=4/4 PASS");
    }

    @Test
    void representativePlansUseAllAuthorizedIndexesAtTenThousandBindings() {
        jdbc.update("""
                INSERT INTO tracking_provider_binding(
                 id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                 created_at,created_by,updated_at,updated_by,provider_type,display_name)
                SELECT gen_random_uuid(),
                 ('00000000-0000-0000-0000-'||lpad((((g-1)%100)+1)::text,12,'0'))::uuid,
                 'binding-scale-key-'||g,'BINDING_'||g,'env:SCALE','ACTIVE',now(),?,now(),?,
                 'FIXTURE','Binding connection '||g FROM generate_series(1,100) g
                """, ACTOR, ACTOR);
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,
                 registered_by,created_at,updated_at)
                SELECT gen_random_uuid(),provider.tenant_id,
                 'legacy-'||provider.provider_alias||'-'||device_number,
                 provider.provider_alias,'ACTIVE',now(),?,now(),now()
                FROM tracking_provider_binding provider CROSS JOIN generate_series(1,100) device_number
                WHERE provider.provider_key_id LIKE 'binding-scale-key-%'
                """, ACTOR);
        jdbc.update("""
                INSERT INTO tracking_device_provider_binding(
                 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,lifecycle,
                 next_poll_at,created_at,updated_at,created_by,updated_by)
                SELECT gen_random_uuid(),device.tenant_id,device.id,provider.id,
                 device.external_device_reference,'ACTIVE',now(),now(),now(),?,?
                FROM tracking_device device JOIN tracking_provider_binding provider
                 ON provider.tenant_id=device.tenant_id AND provider.provider_alias=device.provider_alias
                WHERE provider.provider_key_id LIKE 'binding-scale-key-%'
                """, ACTOR, ACTOR);
        jdbc.execute("ANALYZE tracking_device_provider_binding");
        Map<String, Object> sample = jdbc.queryForMap("""
                SELECT binding.tenant_id,binding.tracking_device_id,binding.provider_binding_id,
                       binding.external_device_reference
                FROM tracking_device_provider_binding binding LIMIT 1
                """);
        String tenant = sample.get("tenant_id").toString();
        String device = sample.get("tracking_device_id").toString();
        String provider = sample.get("provider_binding_id").toString();
        String external = sample.get("external_device_reference").toString();
        assertPlanContains("SELECT external_device_reference FROM tracking_device_provider_binding WHERE tenant_id='" + tenant
                + "'::uuid AND tracking_device_id='" + device + "'::uuid AND lifecycle='ACTIVE'",
                "uq_tracking_device_provider_active_device");
        assertPlanContainsAny("SELECT * FROM tracking_device_provider_binding WHERE tenant_id='" + tenant
                + "'::uuid AND provider_binding_id='" + provider
                + "'::uuid AND external_device_reference='" + external + "'",
                "uq_tracking_device_provider_external",
                "idx_tracking_device_provider_connection_list");
        assertThat(jdbc.queryForObject("""
                SELECT indisvalid FROM pg_index
                WHERE indexrelid='uq_tracking_device_provider_external'::regclass
                """, Boolean.class)).isTrue();
        assertPlanContains("SELECT * FROM tracking_device_provider_binding WHERE tenant_id='" + tenant
                + "'::uuid AND provider_binding_id='" + provider
                + "'::uuid ORDER BY lifecycle,id", "idx_tracking_device_provider_connection_list");
        assertPlanContains("SELECT * FROM tracking_device_provider_binding WHERE lifecycle='ACTIVE' "
                + "AND next_poll_at<=now() ORDER BY next_poll_at LIMIT 100",
                "idx_tracking_device_provider_due");
    }

    private TrackingDeviceProviderBinding activate(
            UUID tenant, TrackingDeviceProviderBinding binding) {
        return store.updateLifecycle(tenant, binding.id(), binding.version(),
                DeviceProviderBindingLifecycle.ACTIVE, ACTOR, NOW.plusSeconds(1));
    }

    private void assertInvalidActivation(UUID tenant, TrackingDeviceProviderBinding binding) {
        assertThatThrownBy(() -> activate(tenant, binding))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_DEVICE_PROVIDER_INVALID"));
    }

    private void assertOneSuccess(List<Object> results) {
        assertThat(results.stream().filter(TrackingDeviceProviderBinding.class::isInstance).count())
                .isEqualTo(1);
        assertThat(results.stream().filter(BusinessRuleException.class::isInstance).count())
                .isEqualTo(1);
    }

    private void assertActiveCount(Fixture fixture, int expected) {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_device_provider_binding
                WHERE tenant_id=? AND tracking_device_id=? AND lifecycle='ACTIVE'
                """, Integer.class, fixture.tenant(), fixture.device())).isEqualTo(expected);
    }

    private Fixture fixture(String suffix) {
        UUID tenant = UUID.randomUUID();
        String alias = "P_" + suffix.toUpperCase().replace('-', '_');
        ProviderConnectionId connection = connection(tenant, alias);
        UUID device = insertDevice(tenant, alias, "legacy-" + suffix, "ACTIVE");
        return new Fixture(tenant, device, connection, alias);
    }

    private ProviderConnectionId connection(UUID tenant, String alias) {
        return connections.create(new NewTrackingProviderConnection(
                tenant, ACTOR, "key-" + UUID.randomUUID(), alias, "env:PROVIDER_REFERENCE",
                ProviderType.of("FIXTURE"), "Connection " + UUID.randomUUID(),
                URI.create("https://provider.example/api"), ProviderSafeConfiguration.empty(),
                5, 500, ProviderConnectionLifecycle.ACTIVE, NOW)).id();
    }

    private NewTrackingDeviceProviderBinding command(
            Fixture fixture, String reference, DeviceProviderBindingLifecycle lifecycle) {
        return new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.device(), fixture.connection(), reference,
                new ProviderSafeConfiguration(Map.of("channel", "safe")), lifecycle,
                null, ACTOR, NOW);
    }

    private UUID insertProvider(UUID tenant, String alias, String lifecycle) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_provider_binding(
                 id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                 created_at,created_by,updated_at,updated_by,provider_type,display_name)
                VALUES(?,?,?,?,?,?,now(),?,now(),?,'FIXTURE',?)
                """, id, tenant, "key-" + UUID.randomUUID(), alias, "env:REFERENCE", lifecycle,
                ACTOR, ACTOR, "Connection " + UUID.randomUUID());
        return id;
    }

    private UUID insertDevice(UUID tenant, String alias, String reference, String lifecycle) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,
                 registered_by,created_at,updated_at)
                VALUES(?,?,?,?,?,now(),?,now(),now())
                """, id, tenant, reference, alias, lifecycle, ACTOR);
        return id;
    }

    private String lifecycle(UUID device) {
        return jdbc.queryForObject("""
                SELECT lifecycle FROM tracking_device_provider_binding WHERE tracking_device_id=?
                """, String.class, device);
    }

    private void migrateTo75() {
        flyway.clean();
        Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("75")).load().migrate();
    }

    private void migrateCurrent() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders()).load().migrate();
    }

    private void assertPlanContains(String query, String index) {
        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN (COSTS OFF) " + query, String.class));
        System.out.printf("US48_PLAN_%s=%s%n", index, plan.replace('\n', ' '));
        assertThat(plan).contains(index);
    }

    private void assertPlanContainsAny(String query, String... indexes) {
        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN (COSTS OFF) " + query, String.class));
        System.out.printf("US48_EXTERNAL_LOOKUP_PLAN=%s%n", plan.replace('\n', ' '));
        assertThat(List.of(indexes).stream().anyMatch(plan::contains)).isTrue();
    }

    private List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            return executor.invokeAll(List.of(
                    () -> callAfterBarrier(barrier, first),
                    () -> callAfterBarrier(barrier, second))).stream().map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            return exception.getCause();
                        }
                    }).toList();
        }
    }

    private Object callAfterBarrier(CyclicBarrier barrier, Callable<?> action) {
        try {
            barrier.await();
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('public.'||?) IS NOT NULL", Boolean.class, table));
    }

    private List<String> columns() {
        return columns("tracking_device_provider_binding");
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name=?
                """, String.class, table);
    }

    private record Fixture(
            UUID tenant, UUID device, ProviderConnectionId connection, String alias) { }
}
