package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;

import java.math.BigDecimal;
import java.time.Duration;

public final class StopAnalysisPolicy {
    public static final BigDecimal MAX_ACCURACY_METERS = new BigDecimal("100.000");
    public static final BigDecimal MAX_STATIONARY_SPEED_KPH = new BigDecimal("3.000");
    public static final BigDecimal MAX_RADIUS_METERS = new BigDecimal("50.000");
    public static final Duration MIN_DWELL = Duration.ofMinutes(5);
    public static final Duration MAX_GAP = Duration.ofMinutes(2);

    private StopAnalysisPolicy() { }

    public static boolean eligible(JourneyPoint point) {
        return point != null && point.trust() == Trust.TRUSTED && point.accuracyMeters() != null
                && point.accuracyMeters().compareTo(MAX_ACCURACY_METERS) <= 0
                && (point.speedKph() == null
                || point.speedKph().compareTo(MAX_STATIONARY_SPEED_KPH) <= 0);
    }

    public static boolean gapAllowed(JourneyPoint previous, JourneyPoint next) {
        return !Duration.between(previous.sourceTimestamp(), next.sourceTimestamp()).isNegative()
                && Duration.between(previous.sourceTimestamp(), next.sourceTimestamp()).compareTo(MAX_GAP) <= 0;
    }

    public static boolean confirmable(StopCandidate candidate) {
        return candidate.dwell().compareTo(MIN_DWELL) >= 0;
    }

    public static boolean mergeable(ConfirmedStop first, ConfirmedStop second, BigDecimal centroidDistance) {
        Duration gap = Duration.between(first.end(), second.start());
        return !gap.isNegative() && gap.compareTo(MAX_GAP) <= 0
                && centroidDistance.compareTo(MAX_RADIUS_METERS) <= 0;
    }
}
