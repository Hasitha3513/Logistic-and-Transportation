package com.transportlogistics.app.tracking.adapters.inbound.telemetry;

import com.transportlogistics.app.tracking.application.GpsReliabilityEvaluationService;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityPolicy;
import com.transportlogistics.app.tracking.ports.outbound.GpsDeviceFreshnessPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class GpsSignalLossScanner {
    private final GpsDeviceFreshnessPort freshness;
    private final GpsReliabilityEvaluationService reliability;
    private final Clock clock;

    GpsSignalLossScanner(GpsDeviceFreshnessPort freshness,
            GpsReliabilityEvaluationService reliability, Clock clock) {
        this.freshness = freshness;
        this.reliability = reliability;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.tracking.gps-reliability.scan-delay:60000}")
    void scan() {
        Instant now = clock.instant();
        freshness.findOfflineCandidates(now.minus(GpsReliabilityPolicy.OFFLINE_LIMIT), 500)
                .forEach(candidate -> reliability.recordSignalLoss(candidate, now));
    }
}
