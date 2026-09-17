package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.GpsReliabilityEvaluationService;
import com.transportlogistics.app.tracking.ports.outbound.GpsDeviceFreshnessPort.DeviceFreshness;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "app.tracking.hybrid-storage.enabled=true",
        "spring.kafka.listener.auto-startup=false",
        "spring.task.scheduling.enabled=false"
})
class GpsExceptionConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final Instant RECEIVED = Instant.parse("2026-09-17T09:00:00Z");

    @Autowired GpsReliabilityEvaluationService reliability;
    @Autowired JdbcTemplate jdbc;
    @MockBean com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEventPublisherPort events;

    @Test
    void concurrentSignalLossDetectionConvergesToOneEpisodeAndOneEvidence() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID device = seedDevice(tenant);
        var candidate = new DeviceFreshness(tenant, device, UUID.randomUUID(), RECEIVED);

        List<Object> results = race(
                () -> record(candidate), () -> record(candidate));

        assertThat(results).containsExactly(Boolean.TRUE, Boolean.TRUE);
        assertThat(count("tracking_gps_exception_episode", tenant)).isOne();
        assertThat(count("tracking_gps_exception_evidence", tenant)).isOne();
        assertThat(jdbc.queryForObject("SELECT evidence_count FROM tracking_gps_exception_episode "
                + "WHERE tenant_id=?", Long.class, tenant)).isOne();
    }

    @Test
    void sameDeviceReferenceRemainsIsolatedAcrossTenantsUnderConcurrentDetection() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID deviceA = seedDevice(tenantA);
        UUID deviceB = seedDevice(tenantB);

        race(() -> record(new DeviceFreshness(tenantA, deviceA, UUID.randomUUID(), RECEIVED)),
                () -> record(new DeviceFreshness(tenantB, deviceB, UUID.randomUUID(), RECEIVED)));

        assertThat(count("tracking_gps_exception_episode", tenantA)).isOne();
        assertThat(count("tracking_gps_exception_episode", tenantB)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_gps_exception_episode "
                + "WHERE tenant_id=? AND tracking_device_id=?", Integer.class, tenantA, deviceB)).isZero();
    }

    private UUID seedDevice(UUID tenant) {
        UUID device = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'GENERIC','ACTIVE',?,?,0,?,?)
                """, device, tenant, "cs08-" + device, Timestamp.from(RECEIVED), UUID.randomUUID(),
                Timestamp.from(RECEIVED), Timestamp.from(RECEIVED));
        return device;
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id=?",
                Integer.class, tenant);
    }

    private boolean record(DeviceFreshness candidate) {
        reliability.recordSignalLoss(candidate, RECEIVED.plusSeconds(301));
        return true;
    }

    private static List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> after(barrier, first));
            var two = executor.submit(() -> after(barrier, second));
            return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        }
    }

    private static Object after(CyclicBarrier barrier, Callable<?> action) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return action.call();
    }
}
