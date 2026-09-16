package com.transportlogistics.app.tracking.domain.gpsedge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Connectivity;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EvaluationContext;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Observation;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Quality;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.SignalState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GpsReliabilityPolicyTest {
    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    void acceptsWgs84BoundariesAndTrailingZeroNormalizedPrecision() {
        assertThat(new GpsCoordinate(new BigDecimal("90.00000000"), new BigDecimal("-180.0000000")))
                .isNotNull();
        assertThatThrownBy(() -> new GpsCoordinate(new BigDecimal("90.00000001"), BigDecimal.ZERO))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("INVALID_COORDINATE");
        assertThatThrownBy(() -> new GpsCoordinate(new BigDecimal("1.12345678"), BigDecimal.ZERO))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("INVALID_COORDINATE_PRECISION");
    }

    @Test
    void classifiesGoodCurrentEvidenceAsTrustedAndDetectorEligible() {
        var result = GpsReliabilityPolicy.assess(observation(50.0, NOW.minusSeconds(10), NOW, 80), context(null));

        assertThat(result.trust()).isEqualTo(Trust.TRUSTED);
        assertThat(result.connectivity()).isEqualTo(Connectivity.LIVE);
        assertThat(result.state()).isEqualTo(ReliabilityState.NORMAL);
        assertThat(result.latestTrustedEligible()).isTrue();
        assertThat(result.detectorEligible()).isTrue();
    }

    @Test
    void protectsTrustedStateFromUnknownLowUnusableAndNullIslandEvidence() {
        var unknown = GpsReliabilityPolicy.assess(observation(null, NOW, NOW, null), context(null));
        var low = GpsReliabilityPolicy.assess(observation(100.01, NOW, NOW, null), context(null));
        var unusable = GpsReliabilityPolicy.assess(observation(1_000.01, NOW, NOW, null), context(null));
        Observation nullIsland = new Observation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), coordinate("0", "0"), 10.0, NOW, NOW, SignalState.UNKNOWN, null);
        var suspect = GpsReliabilityPolicy.assess(nullIsland, context(null));

        assertThat(unknown.trust()).isEqualTo(Trust.UNKNOWN);
        assertThat(unknown.qualities()).contains(Quality.ACCURACY_UNKNOWN);
        assertThat(low.trust()).isEqualTo(Trust.UNTRUSTED);
        assertThat(low.qualities()).contains(Quality.LOW_ACCURACY);
        assertThat(unusable.qualities()).contains(Quality.UNUSABLE_ACCURACY);
        assertThat(suspect.qualities()).contains(Quality.NULL_ISLAND_SUSPECT);
        assertThat(suspect.latestTrustedEligible()).isFalse();
        assertThat(suspect.detectorEligible()).isFalse();
    }

    @Test
    void freezesClockAndFreshnessBoundaries() {
        var toleratedFuture = GpsReliabilityPolicy.assess(
                observation(10.0, NOW.plusSeconds(120), NOW, null), context(null));
        var future = GpsReliabilityPolicy.assess(
                observation(10.0, NOW.plusSeconds(121), NOW, null), context(null));
        var delayed = GpsReliabilityPolicy.assess(
                observation(10.0, NOW.minusSeconds(301), NOW, null), context(null));
        var late = GpsReliabilityPolicy.assess(
                observation(10.0, NOW.minusSeconds(86_401), NOW, null), context(null));
        var offline = GpsReliabilityPolicy.assess(
                observation(10.0, NOW.minusSeconds(601), NOW.minusSeconds(301), null), context(null));

        assertThat(toleratedFuture.qualities()).contains(Quality.CLOCK_SKEW);
        assertThat(future.qualities()).contains(Quality.FUTURE);
        assertThat(delayed.qualities()).contains(Quality.DELAYED);
        assertThat(late.qualities()).contains(Quality.LATE);
        assertThat(offline.connectivity()).isEqualTo(Connectivity.OFFLINE);
        assertThat(offline.state()).isEqualTo(ReliabilityState.OFFLINE);
    }

    @Test
    void detectsEqualTimeConflictAndImpossibleMovementButNotOrdinaryTravel() {
        Observation previous = observation(10.0, NOW.minusSeconds(60), NOW.minusSeconds(60), null);
        Observation equalTimeConflict = new Observation(previous.tenantId(), previous.deviceId(), previous.vehicleId(),
                UUID.randomUUID(), coordinate("6.95", "79.90"), 10.0, previous.sourceTimestamp(), NOW,
                SignalState.UNKNOWN, null);
        Observation impossible = new Observation(previous.tenantId(), previous.deviceId(), previous.vehicleId(),
                UUID.randomUUID(), coordinate("7.20", "80.20"), 10.0, NOW, NOW, SignalState.UNKNOWN, null);
        Observation ordinary = new Observation(previous.tenantId(), previous.deviceId(), previous.vehicleId(),
                UUID.randomUUID(), coordinate("6.9275", "79.8610"), 10.0, NOW, NOW, SignalState.UNKNOWN, null);

        assertThat(GpsReliabilityPolicy.isImpossibleMovement(previous, equalTimeConflict)).isTrue();
        assertThat(GpsReliabilityPolicy.isImpossibleMovement(previous, impossible)).isTrue();
        assertThat(GpsReliabilityPolicy.isImpossibleMovement(previous, ordinary)).isFalse();
    }

    @Test
    void classifiesBatteryThresholdsAndRapidDrainWithoutChangingPositionTrust() {
        var low = GpsReliabilityPolicy.assess(observation(10.0, NOW, NOW, 20), context(null));
        var critical = GpsReliabilityPolicy.assess(observation(10.0, NOW, NOW, 10), context(null));

        assertThat(low.qualities()).contains(Quality.BATTERY_LOW);
        assertThat(low.trust()).isEqualTo(Trust.TRUSTED);
        assertThat(critical.qualities()).contains(Quality.BATTERY_CRITICAL);
        assertThat(GpsReliabilityPolicy.isRapidBatteryDrain(70, NOW.minusSeconds(1_800), 50, NOW)).isTrue();
        assertThat(GpsReliabilityPolicy.isRapidBatteryDrain(70, NOW.minusSeconds(1_801), 50, NOW)).isFalse();
    }

    @Test
    void rejectsInvalidAccuracyBatteryAndExpiredHistory() {
        assertThatThrownBy(() -> observation(0.0, NOW, NOW, null))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("INVALID_ACCURACY");
        assertThatThrownBy(() -> observation(10.0, NOW, NOW, 101))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("INVALID_BATTERY");
        Observation expired = observation(10.0, NOW.minusSeconds(1_000), NOW, null);
        EvaluationContext context = new EvaluationContext(NOW, NOW.minusSeconds(999), null, true, false);
        assertThatThrownBy(() -> GpsReliabilityPolicy.assess(expired, context))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("OUTSIDE_RETENTION");
    }

    private static Observation observation(
            Double accuracy,
            Instant sourceTimestamp,
            Instant receivedAt,
            Integer battery) {
        return new Observation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                coordinate("6.9271", "79.8612"), accuracy, sourceTimestamp, receivedAt,
                SignalState.UNKNOWN, battery);
    }

    private static EvaluationContext context(Observation latestTrusted) {
        return new EvaluationContext(NOW, NOW.minusSeconds(172_800), latestTrusted, true, false);
    }

    private static GpsCoordinate coordinate(String latitude, String longitude) {
        return new GpsCoordinate(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
