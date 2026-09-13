package com.transportlogistics.app.tracking.domain.routedeviation;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record RouteDeviationPosition(UUID positionId, UUID tenantId, UUID vehicleId, Instant sourceTimestamp,
                                     RoutePoint point, DistanceMeters accuracy, boolean accepted, boolean duplicate,
                                     Trust trust, boolean vehicleAssociated, Ordering ordering) {
    public enum Trust { TRUSTED, UNTRUSTED, UNKNOWN }
    public enum Ordering { IN_ORDER, OUT_OF_ORDER, LATE, FUTURE, CLOCK_SKEW }
    public static final Comparator<RouteDeviationPosition> SOURCE_ORDER = Comparator
            .comparing(RouteDeviationPosition::sourceTimestamp).thenComparing(RouteDeviationPosition::positionId);
    public boolean eligible(Instant evaluatedAt) {
        return positionId!=null&&tenantId!=null&&vehicleId!=null&&sourceTimestamp!=null&&point!=null
                &&accuracy!=null&&accuracy.value().compareTo(java.math.BigDecimal.valueOf(1_000))<=0
                &&accepted&&!duplicate&&trust==Trust.TRUSTED&&vehicleAssociated&&ordering==Ordering.IN_ORDER
                &&!sourceTimestamp.isAfter(evaluatedAt)&&Duration.between(sourceTimestamp,evaluatedAt).compareTo(Duration.ofMinutes(5))<=0;
    }
    public DistanceMeters effectiveTolerance(RouteDeviationRule rule){
        if(accuracy==null)throw new RouteDeviationException("ACCURACY_UNKNOWN","Accuracy is required");
        if(accuracy.value().compareTo(java.math.BigDecimal.valueOf(1_000))>0)throw new RouteDeviationException("POSITION_INELIGIBLE","Accuracy exceeds 1000 metres");
        return rule.configuredTolerance().add(accuracy);
    }
}
