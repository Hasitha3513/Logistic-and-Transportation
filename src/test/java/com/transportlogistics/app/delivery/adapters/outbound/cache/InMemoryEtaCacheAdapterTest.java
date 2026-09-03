package com.transportlogistics.app.delivery.adapters.outbound.cache;

import com.transportlogistics.app.delivery.domain.model.EtaSource;
import com.transportlogistics.app.delivery.domain.model.EtaStatus;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEtaCacheAdapterTest {

    private final InMemoryEtaCacheAdapter cache = new InMemoryEtaCacheAdapter();

    @Test
    void rejectsStalePublicationAndPreservesNewerResult() {
        UUID tenant = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        long staleGeneration = cache.beginOrderCalculation(tenant, order);
        cache.evictOrderEta(tenant, order);
        long currentGeneration = cache.beginOrderCalculation(tenant, order);
        SingleOrderEtaEstimate newer = estimate(order, 600);
        SingleOrderEtaEstimate stale = estimate(order, 1200);

        assertThat(cache.putOrderEtaIfCurrent(tenant, order, currentGeneration, "current", newer)).isTrue();
        assertThat(cache.putOrderEtaIfCurrent(tenant, order, staleGeneration, "stale", stale)).isFalse();
        assertThat(cache.getOrderEta(tenant, order, "current")).contains(newer);
        assertThat(cache.getOrderEta(tenant, order, "stale")).isEmpty();
    }

    @Test
    void isolatesSameSubjectIdAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        SingleOrderEtaEstimate estimateA = estimate(order, 300);
        SingleOrderEtaEstimate estimateB = estimate(order, 900);

        assertThat(cache.putOrderEtaIfCurrent(tenantA, order, cache.beginOrderCalculation(tenantA, order), "same", estimateA)).isTrue();
        assertThat(cache.putOrderEtaIfCurrent(tenantB, order, cache.beginOrderCalculation(tenantB, order), "same", estimateB)).isTrue();

        assertThat(cache.getOrderEta(tenantA, order, "same")).contains(estimateA);
        assertThat(cache.getOrderEta(tenantB, order, "same")).contains(estimateB);
    }

    private SingleOrderEtaEstimate estimate(UUID orderId, long durationSeconds) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        return new SingleOrderEtaEstimate(orderId, now.plusSeconds(durationSeconds), durationSeconds,
                durationSeconds * 5, EtaStatus.ON_TIME, EtaSource.HEURISTIC, now, now.plusMinutes(5));
    }
}
