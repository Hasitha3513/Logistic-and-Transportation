package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcTrackingDashboardAuditAdapterTest {
    @Test
    void compactSafeDetailRetainsApprovedFactsWithinExistingColumnBound() {
        String detail = JdbcTrackingDashboardAuditAdapter.detail(UUID.randomUUID().toString(),
                Set.of("VEHICLE", "FRESHNESS", "CONNECTIVITY", "MOTION", "INCIDENT_TYPE", "HEAT_MAP", "INCIDENTS"),
                100, 100,
                Set.of("COORDINATES", "GEOFENCE", "SPEED", "ROUTE_DEVIATION", "JOURNEY_REPLAY"),
                Set.of("LIVE=AVAILABLE", "GEOFENCE=AVAILABLE", "SPEED=AVAILABLE",
                        "ROUTE_DEVIATION=AVAILABLE"));
        assertThat(detail).hasSizeLessThanOrEqualTo(300)
                .contains("f=", "p=100", "r=100", "i=", "s=", "LIVE=AVAILABLE")
                .doesNotContain("[", "]");
    }
}
