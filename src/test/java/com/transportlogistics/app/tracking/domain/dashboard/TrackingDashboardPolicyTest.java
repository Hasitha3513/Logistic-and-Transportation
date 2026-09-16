package com.transportlogistics.app.tracking.domain.dashboard;

import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity.CONNECTED;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness.LIVE;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.MOVING;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.STATIONARY;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.UNKNOWN;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust.TRUSTED;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust.UNTRUSTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingDashboardPolicyTest {
    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @Test
    void validatesTenantVehicleAndPageBounds() {
        var valid = new DashboardQuery(UUID.randomUUID(), filter(Set.of()), null, 100);
        assertThat(TrackingDashboardPolicy.validate(valid)).isSameAs(valid);

        var vehicleIds = new HashSet<UUID>();
        for (int index = 0; index < 101; index++) {
            vehicleIds.add(UUID.randomUUID());
        }
        assertThatThrownBy(() -> TrackingDashboardPolicy.validate(
                new DashboardQuery(valid.tenantId(), filter(vehicleIds), null, 50)))
                .isInstanceOf(TrackingDashboardException.class)
                .extracting("code").isEqualTo("TRACKING_DASHBOARD_QUERY_INVALID");
        assertThatThrownBy(() -> TrackingDashboardPolicy.validate(
                new DashboardQuery(valid.tenantId(), filter(Set.of()), null, 101)))
                .isInstanceOf(TrackingDashboardException.class);
    }

    @Test
    void classifiesOnlyKnownTrustedSpeedAsMotion() {
        assertThat(TrackingDashboardPolicy.motion(observation(TRUSTED, "3.001", "6.927", "79.861")))
                .isEqualTo(MOVING);
        assertThat(TrackingDashboardPolicy.motion(observation(TRUSTED, "3.0", "6.927", "79.861")))
                .isEqualTo(STATIONARY);
        assertThat(TrackingDashboardPolicy.motion(observation(TRUSTED, null, "6.927", "79.861")))
                .isEqualTo(UNKNOWN);
        assertThat(TrackingDashboardPolicy.motion(observation(UNTRUSTED, "25", "6.927", "79.861")))
                .isEqualTo(UNKNOWN);
        assertThat(TrackingDashboardPolicy.motion(null)).isEqualTo(UNKNOWN);
    }

    @Test
    void heatMapAcceptsOnlyTrustedLiveOrRecentCoordinates() {
        var trusted = observation(TRUSTED, "10", "6.927", "79.861");
        assertThat(TrackingDashboardPolicy.heatEligible(LIVE, trusted)).isTrue();
        assertThat(TrackingDashboardPolicy.heatEligible(
                com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness.STALE,
                trusted)).isFalse();
        assertThat(TrackingDashboardPolicy.heatEligible(LIVE,
                observation(UNTRUSTED, "10", "6.927", "79.861"))).isFalse();
    }

    @Test
    void binsCoordinatesIntoDeterministicOneHundredthDegreeCells() {
        var positive = TrackingDashboardPolicy.heatCell(
                observation(TRUSTED, "10", "6.927", "79.861"), 3);
        assertThat(positive.latitude()).isEqualByComparingTo("6.925");
        assertThat(positive.longitude()).isEqualByComparingTo("79.865");
        assertThat(positive.count()).isEqualTo(3);

        var negative = TrackingDashboardPolicy.heatCell(
                observation(TRUSTED, "10", "-6.927", "-79.861"), 1);
        assertThat(negative.latitude()).isEqualByComparingTo("-6.925");
        assertThat(negative.longitude()).isEqualByComparingTo("-79.865");
    }

    @Test
    void filterCollectionsAreDefensivelyCopiedAndNullMeansNoFilter() {
        var ids = new HashSet<UUID>();
        var id = UUID.randomUUID();
        ids.add(id);
        var filter = filter(ids);
        ids.clear();
        assertThat(filter.vehicleIds()).containsExactly(id);

        var empty = new DashboardFilter(null, null, null, null, null, false, true);
        assertThat(empty.vehicleIds()).isEmpty();
        assertThat(empty.freshness()).isEmpty();
    }

    private static DashboardFilter filter(Set<UUID> vehicleIds) {
        return new DashboardFilter(vehicleIds, Set.of(LIVE), Set.of(CONNECTED), Set.of(), Set.of(), true, true);
    }

    private static Observation observation(
            TrackingDashboardModels.Trust trust, String speed, String latitude, String longitude) {
        return new Observation(NOW, NOW, trust,
                latitude == null ? null : new BigDecimal(latitude),
                longitude == null ? null : new BigDecimal(longitude),
                BigDecimal.TEN,
                speed == null ? null : new BigDecimal(speed));
    }
}
