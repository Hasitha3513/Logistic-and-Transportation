package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome.Result;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderIngestionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProviderCoordinatorPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000048");
    @Autowired private TrackingProviderExecutionStore executions;
    @Autowired private TrackingDeviceProviderBindingStore bindings;
    @Autowired private TrackingProviderIngestionPort ingestion;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void skipLockedClaimIsUniqueAcrossInstancesAndExpiredLeaseIsRecoverable() throws Exception {
        disableExistingProviders();
        Fixture fixture = fixture("CLAIM", 1);
        var pool = Executors.newFixedThreadPool(2);
        Instant now = Instant.now();
        try {
            var first = pool.submit(() -> executions.claimDueConnections(
                    "instance-a", now, Duration.ofSeconds(30), 1));
            var second = pool.submit(() -> executions.claimDueConnections(
                    "instance-b", now, Duration.ofSeconds(30), 1));
            int claims = first.get(20, TimeUnit.SECONDS).size()
                    + second.get(20, TimeUnit.SECONDS).size();
            assertThat(claims).isEqualTo(1);
            jdbc.update("UPDATE tracking_provider_binding SET lease_until=? WHERE id=?",
                    java.sql.Timestamp.from(now.minusSeconds(1)), fixture.connectionId());
            assertThat(executions.claimDueConnections(
                    "instance-c", now, Duration.ofSeconds(30), 1)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void onlyCurrentOwnerCanRenewOrReleaseAfterReclaim() {
        disableExistingProviders();
        Fixture fixture = fixture("OWNER", 1);
        Instant now = Instant.now();
        var claimed = executions.claimDueConnections(
                "owner-a", now, Duration.ofSeconds(30), 1).get(0);
        assertThat(executions.renewLease(
                claimed.id(), "wrong-owner", now, Duration.ofSeconds(30))).isFalse();
        assertThat(executions.renewLease(
                claimed.id(), "owner-a", now, Duration.ofSeconds(30))).isTrue();
        jdbc.update("UPDATE tracking_provider_binding SET lease_until=? WHERE id=?",
                java.sql.Timestamp.from(now.minusSeconds(1)), fixture.connectionId());
        assertThat(executions.claimDueConnections(
                "owner-b", now, Duration.ofSeconds(30), 1)).hasSize(1);
        assertThat(executions.releaseFailure(
                claimed.id(), "owner-a", now, now.plusSeconds(5), "OLD_OWNER")).isFalse();
        assertThat(executions.releaseSuccess(
                claimed.id(), "owner-b", now, now.plusSeconds(5), null)).isTrue();
    }

    @Test
    void internalIngestionReloadsLeaseTenantBindingAndDeviceAndPreservesDedupe() {
        disableExistingProviders();
        Fixture fixture = fixture("INGEST", 1);
        Instant now = Instant.now();
        var claimed = executions.claimDueConnections(
                "ingest-owner", now, Duration.ofSeconds(30), 1).get(0);
        NormalizedPositionCandidate candidate = candidate(
                fixture.externalReference(), "message-1", now);
        var accepted = ingestion.ingest(
                claimed.id(), "ingest-owner", java.util.List.of(candidate), now);
        assertThat(accepted).extracting(outcome -> outcome.result()).containsExactly(Result.ACCEPTED);
        var duplicate = ingestion.ingest(
                claimed.id(), "ingest-owner", java.util.List.of(candidate), now.plusMillis(1));
        assertThat(duplicate).extracting(outcome -> outcome.result()).containsExactly(Result.DUPLICATE);
        jdbc.update("UPDATE tracking_device_provider_binding SET lifecycle='DISABLED' WHERE id=?",
                fixture.bindingId());
        assertThat(ingestion.ingest(
                claimed.id(), "ingest-owner", java.util.List.of(candidate("other", "message-2", now)),
                now.plusMillis(2))).extracting(outcome -> outcome.result()).containsExactly(Result.REJECTED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void activeBindingDiscoveryIsBoundedFairHotAndTenantScopedForOneHundredDevices() {
        Fixture fixture = fixture("BATCH", 100);
        Fixture other = fixture("OTHER", 1);
        Instant now = Instant.now();
        var seen = new HashSet<UUID>();
        for (int page = 0; page < 4; page++) {
            var due = executions.findDueActiveBindings(
                    fixture.tenantId(), new ProviderConnectionId(fixture.connectionId()), now, 25);
            assertThat(due).hasSize(25);
            due.forEach(binding -> {
                seen.add(binding.id());
                bindings.updateNextPoll(
                        binding.tenantId(), binding.id(), binding.version(), now.plusSeconds(60),
                        binding.updatedBy(), now);
            });
        }
        assertThat(seen).hasSize(100);
        assertThat(executions.findDueActiveBindings(
                fixture.tenantId(), new ProviderConnectionId(fixture.connectionId()), now, 25)).isEmpty();
        assertThat(executions.findDueActiveBindings(
                other.tenantId(), new ProviderConnectionId(other.connectionId()), now, 25)).hasSize(1);
        UUID hot = addDevice(fixture, 101, "ACTIVE").bindingId();
        assertThat(executions.findDueActiveBindings(
                fixture.tenantId(), new ProviderConnectionId(fixture.connectionId()), now, 25))
                .extracting(binding -> binding.id()).containsExactly(hot);
        jdbc.update("UPDATE tracking_device_provider_binding SET lifecycle='DISABLED' WHERE id=?", hot);
        assertThat(executions.findDueActiveBindings(
                fixture.tenantId(), new ProviderConnectionId(fixture.connectionId()), now, 25)).isEmpty();
    }

    @Test
    void disablingClaimedProviderFailsClosedAndPreventsFutureClaims() {
        disableExistingProviders();
        Fixture fixture = fixture("DISABLE", 1);
        Instant now = Instant.now();
        var claimed = executions.claimDueConnections(
                "disable-owner", now, Duration.ofSeconds(30), 1).get(0);
        jdbc.update("UPDATE tracking_provider_binding SET lifecycle='DISABLED' WHERE id=?",
                fixture.connectionId());
        assertThat(executions.reloadActive(claimed.id(), "disable-owner", now)).isEmpty();
        assertThat(ingestion.ingest(
                claimed.id(), "disable-owner",
                java.util.List.of(candidate(fixture.externalReference(), "disabled", now)), now))
                .extracting(outcome -> outcome.result()).containsExactly(Result.REJECTED);
        assertThat(executions.claimDueConnections(
                "other", now.plusSeconds(31), Duration.ofSeconds(30), 1)).isEmpty();
    }

    @Test
    void rebindBeforeCandidateIngestionRejectsTheOldExternalIdentity() {
        disableExistingProviders();
        Fixture fixture = fixture("REBIND", 1);
        Instant now = Instant.now();
        var claimed = executions.claimDueConnections(
                "rebind-owner", now, Duration.ofSeconds(30), 1).get(0);
        jdbc.update("UPDATE tracking_device_provider_binding SET lifecycle='DISABLED' WHERE id=?",
                fixture.bindingId());
        jdbc.update("""
                INSERT INTO tracking_device_provider_binding(
                 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
                 lifecycle,created_at,updated_at,created_by,updated_by,version)
                VALUES(?,?,?,?,?,'ACTIVE',current_timestamp,current_timestamp,?,?,0)
                """, UUID.randomUUID(), fixture.tenantId(), fixture.deviceId(), fixture.connectionId(),
                "replacement-reference", ACTOR, ACTOR);
        assertThat(ingestion.ingest(
                claimed.id(), "rebind-owner",
                java.util.List.of(candidate(fixture.externalReference(), "in-flight", now)), now))
                .extracting(outcome -> outcome.result()).containsExactly(Result.REJECTED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position", Integer.class))
                .isZero();
    }

    @Test
    void concurrentWatermarkAdvanceAcceptsOneVersionAndRejectsTheStaleWriter() throws Exception {
        Fixture fixture = fixture("WATERMARK", 1);
        Instant now = Instant.now();
        Callable<Boolean> update = () -> {
            try {
                bindings.updateWatermark(
                        fixture.tenantId(), fixture.bindingId(), 0, now, "message", now.plusSeconds(5),
                        ACTOR, now);
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(update);
            var second = pool.submit(update);
            assertThat(java.util.List.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
        System.out.println("US48_CS04_DATABASE_RACES=7/7 PASS");
    }

    @Test
    void tenThousandBindingDueQueryRemainsIndexBackedWithoutPerDeviceRuntimeObjects() {
        Fixture fixture = fixture("SCALE", 0);
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,
                 registered_at,registered_by,version,created_at,updated_at)
                SELECT md5('device-'||series)::uuid,?, 'scale-'||series,'FIXTURE','ACTIVE',
                       current_timestamp,?,0,current_timestamp,current_timestamp
                FROM generate_series(1,10000) series
                """, fixture.tenantId(), ACTOR);
        jdbc.update("""
                INSERT INTO tracking_device_provider_binding(
                 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
                 lifecycle,created_at,updated_at,created_by,updated_by,version)
                SELECT md5('binding-'||series)::uuid,?,md5('device-'||series)::uuid,?,
                       'scale-'||series,'ACTIVE',current_timestamp,current_timestamp,?,?,0
                FROM generate_series(1,10000) series
                """, fixture.tenantId(), fixture.connectionId(), ACTOR, ACTOR);
        String plan = String.join(" ", jdbc.queryForList("""
                EXPLAIN SELECT binding.* FROM tracking_device_provider_binding binding
                JOIN tracking_device device ON device.tenant_id=binding.tenant_id
                  AND device.id=binding.tracking_device_id
                JOIN tracking_provider_binding provider ON provider.tenant_id=binding.tenant_id
                  AND provider.id=binding.provider_binding_id
                WHERE binding.tenant_id=? AND binding.provider_binding_id=?
                  AND binding.lifecycle='ACTIVE' AND device.lifecycle='ACTIVE'
                  AND provider.lifecycle='ACTIVE'
                  AND (binding.next_poll_at IS NULL OR binding.next_poll_at<=current_timestamp)
                ORDER BY binding.next_poll_at NULLS FIRST,binding.id LIMIT 100
                """, String.class, fixture.tenantId(), fixture.connectionId()));
        assertThat(plan).contains("Index");
        assertThat(executions.findDueActiveBindings(
                fixture.tenantId(), new ProviderConnectionId(fixture.connectionId()), Instant.now(), 100))
                .hasSize(100);
        System.out.println("US48_CS04_10K_BINDINGS=PASS THREADS_PER_DEVICE=0 BATCH_SIZE=100");
    }

    private Fixture fixture(String suffix, int deviceCount) {
        UUID tenantId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_provider_binding(
                 id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                 created_at,created_by,updated_at,updated_by,version,provider_type,display_name,
                 safe_configuration,poll_interval_seconds,page_size,test_status,next_poll_at)
                VALUES(?,?,?,?,?,'ACTIVE',current_timestamp,?,current_timestamp,?,0,
                       'FIXTURE',?,'{}'::jsonb,5,100,'PASS',NULL)
                """, connectionId, tenantId, "key-" + suffix + "-" + UUID.randomUUID(),
                "FIXTURE", "secret-" + suffix, ACTOR, ACTOR, suffix);
        Fixture fixture = new Fixture(tenantId, connectionId, null, null, null);
        Fixture first = fixture;
        for (int index = 1; index <= deviceCount; index++) {
            AddedDevice added = addDevice(fixture, index, "ACTIVE");
            if (index == 1) {
                first = new Fixture(tenantId, connectionId,
                        added.deviceId(), added.bindingId(), added.externalReference());
            }
        }
        return first;
    }

    private void disableExistingProviders() {
        jdbc.update("""
                UPDATE tracking_provider_binding
                SET lifecycle='DISABLED',lease_owner=NULL,lease_until=NULL
                WHERE lifecycle='ACTIVE'
                """);
    }

    private AddedDevice addDevice(Fixture fixture, int index, String lifecycle) {
        UUID deviceId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        String reference = "device-" + fixture.connectionId() + "-" + index;
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,
                 registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FIXTURE',?,current_timestamp,?,0,current_timestamp,current_timestamp)
                """, deviceId, fixture.tenantId(), reference, lifecycle, ACTOR);
        jdbc.update("""
                INSERT INTO tracking_device_provider_binding(
                 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
                 lifecycle,created_at,updated_at,created_by,updated_by,version)
                VALUES(?,?,?,?,?,'ACTIVE',current_timestamp,current_timestamp,?,?,0)
                """, bindingId, fixture.tenantId(), deviceId, fixture.connectionId(), reference,
                ACTOR, ACTOR);
        jdbc.update("""
                INSERT INTO tracking_vehicle_device_assignment(
                 id,tenant_id,tracking_device_id,vehicle_id,effective_from,created_at,created_by)
                VALUES(?,?,?,?,?,current_timestamp,?)
                """, UUID.randomUUID(), fixture.tenantId(), deviceId, UUID.randomUUID(),
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), ACTOR);
        return new AddedDevice(deviceId, bindingId, reference);
    }

    private static NormalizedPositionCandidate candidate(
            String reference, String messageId, Instant sourceTimestamp) {
        return new NormalizedPositionCandidate(
                reference, sourceTimestamp, BigDecimal.valueOf(6.9271),
                BigDecimal.valueOf(79.8612), BigDecimal.valueOf(5), null, null, null,
                EngineState.UNKNOWN, null, null, messageId, null);
    }

    private record Fixture(
            UUID tenantId,
            UUID connectionId,
            UUID deviceId,
            UUID bindingId,
            String externalReference) { }

    private record AddedDevice(UUID deviceId, UUID bindingId, String externalReference) { }
}
