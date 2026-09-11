package com.transportlogistics.app.tracking.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeofencePositionEligibilityTest {
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Test
    void acceptsTrustedInOrderPositionAtFiveMinuteBoundary() {
        assertThat(GeofencePositionEligibility.isEligible(position(Trust.TRUSTED,
                Ordering.IN_ORDER, false, NOW.minusSeconds(300), UUID.randomUUID()), NOW)).isTrue();
    }

    @Test
    void rejectsDuplicateUntrustedOutOfOrderStaleFutureAndMissingVehicle() {
        assertThat(eligible(Trust.TRUSTED, Ordering.IN_ORDER, true, NOW)).isFalse();
        assertThat(eligible(Trust.UNTRUSTED, Ordering.IN_ORDER, false, NOW)).isFalse();
        assertThat(eligible(Trust.TRUSTED, Ordering.OUT_OF_ORDER, false, NOW)).isFalse();
        assertThat(eligible(Trust.TRUSTED, Ordering.IN_ORDER, false, NOW.minusSeconds(301))).isFalse();
        assertThat(eligible(Trust.TRUSTED, Ordering.IN_ORDER, false, NOW.plusNanos(1))).isFalse();
        assertThat(GeofencePositionEligibility.isEligible(position(Trust.TRUSTED,
                Ordering.IN_ORDER, false, NOW, null), NOW)).isFalse();
    }

    private static boolean eligible(Trust trust, Ordering ordering, boolean duplicate, Instant source) {
        return GeofencePositionEligibility.isEligible(
                position(trust, ordering, duplicate, source, UUID.randomUUID()), NOW);
    }

    private static GeofencePosition position(Trust trust, Ordering ordering, boolean duplicate,
                                             Instant source, UUID vehicleId) {
        return new GeofencePosition(UUID.randomUUID(), UUID.randomUUID(), vehicleId, source,
                new Wgs84Coordinate(1, 1), trust, ordering, duplicate);
    }
}
