package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import java.time.Instant;
import java.util.UUID;

public interface SpeedingEpisodePublisherPort {
    void publish(UUID tenantId, VehicleSpeedingDetectedV1 event);

    record VehicleSpeedingDetectedV1(UUID speedEpisodeId, UUID vehicleId,
                                     UUID driverId, UUID tripId, UUID routeId, String routeVersion,
                                     SpeedKph observedSpeedKph, SpeedKph effectiveThresholdKph,
                                     ResolvedSpeedThreshold.ThresholdSource thresholdSource,
                                     UUID ruleId, long ruleVersion,
                                     SpeedingEpisode.Severity severity,
                                     Instant sourceTimestamp, int repeatCount) {
    }
}
