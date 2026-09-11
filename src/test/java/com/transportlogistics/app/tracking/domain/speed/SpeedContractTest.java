package com.transportlogistics.app.tracking.domain.speed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import com.transportlogistics.app.trip.VehicleTripAssignmentLookup;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpeedContractTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    @Test
    void eligibilityReasonsAreExplicit() {
        assertEquals(SpeedPosition.Eligibility.DUPLICATE, position("60", true, true, true, true, NOW)
                .eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.UNTRUSTED, position("60", false, false, true, true, NOW)
                .eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.UNASSOCIATED, position("60", false, true, false, true, NOW)
                .eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.OUT_OF_ORDER, position("60", false, true, true, false, NOW)
                .eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.FUTURE, position("60", false, true, true, true,
                NOW.plusSeconds(1)).eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.ELIGIBLE, position("60", false, true, true, true,
                NOW.minusSeconds(300)).eligibilityAt(NOW));
        assertEquals(SpeedPosition.Eligibility.STALE, position("60", false, true, true, true,
                NOW.minusSeconds(301)).eligibilityAt(NOW));
    }

    @Test
    void attributionSupportsKnownPartialAndUnknownFacts() {
        SpeedAttribution known = new SpeedAttribution(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "R1");
        assertFalse(known.matchesRoute(UUID.randomUUID(), "R1"));
        SpeedAttribution unknown = SpeedAttribution.unknown();
        assertNull(unknown.tripId());
        assertNull(unknown.driverId());
        assertNull(unknown.routeId());
        assertNull(unknown.routeVersion());
        VehicleTripAssignmentLookup.VehicleTripAssignment partial =
                new VehicleTripAssignmentLookup.VehicleTripAssignment(UUID.randomUUID(), null, null, null);
        assertNull(partial.driverId());
    }

    @Test
    void publicationContractHasExactMinimizedFields() {
        List<String> names = Arrays.stream(SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1.class
                        .getRecordComponents()).map(RecordComponent::getName).toList();
        assertEquals(List.of("speedEpisodeId", "vehicleId", "driverId", "tripId",
                "routeId", "routeVersion", "observedSpeedKph", "effectiveThresholdKph",
                "thresholdSource", "ruleId", "ruleVersion", "severity", "sourceTimestamp",
                "repeatCount"), names);
    }

    private static SpeedPosition position(String speed, boolean duplicate, boolean trusted,
                                          boolean associated, boolean inOrder, Instant sourceTime) {
        return new SpeedPosition(TENANT, VEHICLE, UUID.randomUUID(), sourceTime,
                speed == null ? null : new SpeedKph(new BigDecimal(speed)), duplicate, trusted,
                associated, inOrder);
    }
}
