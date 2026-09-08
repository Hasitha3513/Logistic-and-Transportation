package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Associate;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.CreateDevice;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.PositionCommand;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.ProviderContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TrackingConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final String PROVIDER = "FIXTURE";
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired TrackingStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void race1ExactDuplicatePacketHasOneHistoryFact() throws Exception {
        var fixture = fixture();
        var command = position(fixture.deviceId(), "duplicate", 1L, Instant.now(), "6.9271000");
        var results = race(() -> ingest(fixture, command), () -> ingest(fixture, command));
        assertThat(results).allMatch(value -> value instanceof List<?>);
        assertThat(count("tracking_position", fixture.tenantId())).isEqualTo(1);
    }

    @Test
    void race2IdentityConflictPreservesOriginal() throws Exception {
        var fixture = fixture();
        ingest(fixture, position(fixture.deviceId(), "conflict", 1L, Instant.now(), "6.9271000"));
        var results = race(
                () -> ingest(fixture, position(fixture.deviceId(), "conflict", 1L, Instant.now(), "6.9272000")),
                () -> ingest(fixture, position(fixture.deviceId(), "conflict", 1L, Instant.now(), "6.9273000")));
        assertThat(results).allMatch(BusinessRuleException.class::isInstance);
        assertThat(count("tracking_position", fixture.tenantId())).isEqualTo(1);
    }

    @Test
    void race3NewerObservationWinsWhenArrivalOrderCompetes() throws Exception {
        var fixture = fixture();
        var base = Instant.now().minusSeconds(30);
        race(
                () -> ingest(fixture, position(fixture.deviceId(), "older", 1L, base, "6.9271000")),
                () -> ingest(fixture, position(fixture.deviceId(), "newer", 2L, base.plusSeconds(10), "6.9272000")));
        assertThat(store.state(fixture.tenantId(), fixture.vehicleId(), Instant.now()).orElseThrow()
                        .latestTrusted().providerMessageId())
                .isEqualTo("newer");
        assertThat(count("tracking_position", fixture.tenantId())).isEqualTo(2);
    }

    @Test
    void race4DoubleActiveAssociationHasOneWinner() throws Exception {
        var tenant = UUID.randomUUID();
        var device = createDevice(tenant, "double-association");
        var start = Instant.now().minusSeconds(60);
        var results = race(
                () -> store.associate(context(tenant), device, new Associate(UUID.randomUUID(), start), Instant.now()),
                () -> store.associate(context(tenant), device, new Associate(UUID.randomUUID(), start), Instant.now()));
        assertThat(results.stream().filter(value -> value instanceof BusinessRuleException).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_vehicle_device_assignment WHERE tenant_id=? AND effective_to IS NULL", Integer.class, tenant)).isEqualTo(1);
    }

    @Test
    void race5ReassignmentUsesAssociationAtSourceTime() {
        var fixture = fixture(Instant.now().minusSeconds(7200));
        var boundary = Instant.now().minusSeconds(1800);
        var associationId = jdbc.queryForObject("SELECT id FROM tracking_vehicle_device_assignment WHERE tenant_id=?", UUID.class, fixture.tenantId());
        store.endAssociation(context(fixture.tenantId()), fixture.deviceId(), associationId, boundary, Instant.now());
        var newVehicle = UUID.randomUUID();
        store.associate(context(fixture.tenantId()), fixture.deviceId(), new Associate(newVehicle, boundary), Instant.now());
        ingest(fixture, position(fixture.deviceId(), "historical", 1L, boundary.minusSeconds(1), "6.9271000"));
        assertThat(jdbc.queryForObject("SELECT vehicle_id FROM tracking_position WHERE tenant_id=?", UUID.class, fixture.tenantId())).isEqualTo(fixture.vehicleId());
    }

    @Test
    void race6EqualTimestampUsesProviderSequenceThenIdentity() throws Exception {
        var fixture = fixture();
        var source = Instant.now().minusSeconds(10);
        race(
                () -> ingest(fixture, position(fixture.deviceId(), "sequence-1", 1L, source, "6.9271000")),
                () -> ingest(fixture, position(fixture.deviceId(), "sequence-2", 2L, source, "6.9272000")));
        assertThat(store.state(fixture.tenantId(), fixture.vehicleId(), Instant.now()).orElseThrow()
                        .latestTrusted().providerSequence())
                .isEqualTo(2L);
    }

    @Test
    void race7ExternalReferenceIsTenantScoped() throws Exception {
        var reference = "shared-external-reference";
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        var results = race(() -> createDevice(tenantA, reference), () -> createDevice(tenantB, reference));
        assertThat(results).allMatch(UUID.class::isInstance);
    }

    @Test
    void race8DuplicateWithinBatchAndConcurrentBatchProducesOneFact() throws Exception {
        var fixture = fixture();
        var packet = position(fixture.deviceId(), "batch-duplicate", 8L, Instant.now(), "6.9271000");
        var results = race(
                () -> store.ingest(provider(fixture), List.of(packet, packet), Instant.now()),
                () -> store.ingest(provider(fixture), List.of(packet), Instant.now()));
        assertThat(results).allMatch(value -> value instanceof List<?>);
        assertThat(count("tracking_position", fixture.tenantId())).isEqualTo(1);
    }

    @Test
    void race9ForcedRollbackLeavesNoHistoryOrProjection() {
        var fixture = fixture();
        var transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            ingest(fixture, position(fixture.deviceId(), "rollback", 9L, Instant.now(), "6.9271000"));
            throw new IllegalStateException("forced rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count("tracking_position", fixture.tenantId())).isZero();
        assertThat(count("tracking_vehicle_latest", fixture.tenantId())).isZero();
    }

    @Test
    void databaseRejectsHistoryMutationAndOverlappingClosedIntervals() {
        var fixture = fixture(Instant.now().minusSeconds(3600));
        ingest(fixture, position(fixture.deviceId(), "immutable", 1L, Instant.now(), "6.9271000"));
        assertThatThrownBy(() -> jdbc.update("UPDATE tracking_position SET latitude=0 WHERE tenant_id=?", fixture.tenantId())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM tracking_position WHERE tenant_id=?", fixture.tenantId())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO tracking_vehicle_device_assignment(id,tenant_id,tracking_device_id,vehicle_id,effective_from,effective_to,created_at,created_by) VALUES(?,?,?,?,?,?,?,?)", UUID.randomUUID(), fixture.tenantId(), fixture.deviceId(), UUID.randomUUID(), java.sql.Timestamp.from(Instant.now().minusSeconds(1800)), java.sql.Timestamp.from(Instant.now().plusSeconds(1800)), java.sql.Timestamp.from(Instant.now()), ACTOR)).isInstanceOf(Exception.class);
    }

    private Fixture fixture() {
        return fixture(Instant.now().minusSeconds(600));
    }

    private Fixture fixture(Instant effectiveFrom) {
        var tenant = UUID.randomUUID();
        var device = createDevice(tenant, "device-" + tenant);
        var vehicle = UUID.randomUUID();
        store.associate(context(tenant), device, new Associate(vehicle, effectiveFrom), Instant.now());
        return new Fixture(tenant, device, vehicle);
    }

    private UUID createDevice(UUID tenant, String reference) {
        return store.insertDevice(context(tenant), new CreateDevice(reference, PROVIDER, null), Instant.now()).id();
    }

    private List<?> ingest(Fixture fixture, PositionCommand command) {
        return store.ingest(provider(fixture), List.of(command), Instant.now());
    }

    private ProviderContext provider(Fixture fixture) {
        return new ProviderContext(fixture.tenantId(), PROVIDER);
    }

    private Context context(UUID tenant) {
        return new Context(tenant, ACTOR, "us48-test");
    }

    private PositionCommand position(UUID device, String messageId, Long sequence, Instant source, String latitude) {
        return new PositionCommand(device, messageId, sequence, source, new BigDecimal(latitude), new BigDecimal("79.8612000"), new BigDecimal("5"), new BigDecimal("42"), new BigDecimal("90"), null, EngineState.ON, null, null, java.util.Map.of("fixture", "controlled"));
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id=?", Integer.class, tenant);
    }

    private List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        var barrier = new CyclicBarrier(2);
        Callable<Object> guardedFirst = () -> callAfterBarrier(barrier, first);
        Callable<Object> guardedSecond = () -> callAfterBarrier(barrier, second);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = executor.invokeAll(List.of(guardedFirst, guardedSecond));
            return futures.stream().map(future -> {
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

    private record Fixture(UUID tenantId, UUID deviceId, UUID vehicleId) {}
}
