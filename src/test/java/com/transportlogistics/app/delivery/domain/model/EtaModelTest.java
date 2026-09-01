package com.transportlogistics.app.delivery.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtaModelTest {

    @Test
    @DisplayName("SingleOrderEtaEstimate should construct and detect staleness correctly")
    void singleOrderEtaEstimate_valid_succeeds() {
        UUID orderId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime arrival = now.plusMinutes(20);
        OffsetDateTime staleAt = now.plusMinutes(15);

        SingleOrderEtaEstimate estimate = new SingleOrderEtaEstimate(
                orderId,
                arrival,
                1200L,
                5000L,
                EtaStatus.ON_TIME,
                EtaSource.HEURISTIC,
                now,
                staleAt
        );

        assertThat(estimate.orderId()).isEqualTo(orderId);
        assertThat(estimate.travelDurationSeconds()).isEqualTo(1200L);
        assertThat(estimate.distanceMeters()).isEqualTo(5000L);
        assertThat(estimate.slaStatus()).isEqualTo(EtaStatus.ON_TIME);
        assertThat(estimate.source()).isEqualTo(EtaSource.HEURISTIC);
        assertThat(estimate.isStale(now)).isFalse();
        assertThat(estimate.isStale(now.plusMinutes(16))).isTrue();
    }

    @Test
    @DisplayName("SingleOrderEtaEstimate should validate mandatory fields")
    void singleOrderEtaEstimate_invalid_throws() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        assertThatThrownBy(() -> new SingleOrderEtaEstimate(
                null,
                now,
                100L,
                1000L,
                EtaStatus.ON_TIME,
                EtaSource.HEURISTIC,
                now,
                now.plusMinutes(15)
        )).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BatchEtaEstimate should construct with stops and detect staleness")
    void batchEtaEstimate_valid_succeeds() {
        UUID batchId = UUID.randomUUID();
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        BatchEtaStopEstimate stop1 = new BatchEtaStopEstimate(
                order1,
                1,
                now.plusMinutes(10),
                600L,
                300L,
                2500L,
                EtaStatus.ON_TIME
        );

        BatchEtaStopEstimate stop2 = new BatchEtaStopEstimate(
                order2,
                2,
                now.plusMinutes(25),
                600L,
                300L,
                2500L,
                EtaStatus.AT_RISK
        );

        BatchEtaEstimate batchEstimate = new BatchEtaEstimate(
                batchId,
                now,
                now.plusMinutes(15),
                1800L,
                5000L,
                now.plusMinutes(30),
                EtaSource.HEURISTIC,
                List.of(stop1, stop2)
        );

        assertThat(batchEstimate.batchId()).isEqualTo(batchId);
        assertThat(batchEstimate.stops()).hasSize(2);
        assertThat(batchEstimate.totalDurationSeconds()).isEqualTo(1800L);
        assertThat(batchEstimate.totalDistanceMeters()).isEqualTo(5000L);
        assertThat(batchEstimate.isStale(now)).isFalse();
        assertThat(batchEstimate.isStale(now.plusMinutes(20))).isTrue();
    }
}
