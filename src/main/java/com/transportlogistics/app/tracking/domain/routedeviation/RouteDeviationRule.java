package com.transportlogistics.app.tracking.domain.routedeviation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RouteDeviationRule(UUID id, UUID tenantId, UUID routeId, RouteVersion routeVersion,
                                 DistanceMeters configuredTolerance, Lifecycle lifecycle,
                                 long ruleVersion, long manageVersion, Instant effectiveAt) {
    public enum Lifecycle { DRAFT, ACTIVE, DISABLED, RETIRED }
    public RouteDeviationRule {
        Objects.requireNonNull(id); Objects.requireNonNull(tenantId); Objects.requireNonNull(routeId);
        Objects.requireNonNull(routeVersion); Objects.requireNonNull(configuredTolerance); Objects.requireNonNull(lifecycle);
        if (configuredTolerance.value().compareTo(java.math.BigDecimal.TEN) < 0
                || configuredTolerance.value().compareTo(java.math.BigDecimal.valueOf(5_000)) > 0)
            throw new RouteDeviationException("INVALID_TOLERANCE", "Tolerance must be between 10 and 5000 metres");
        if (ruleVersion < 0 || manageVersion < 0) throw new IllegalArgumentException("Versions cannot be negative");
        if (lifecycle == Lifecycle.ACTIVE && (ruleVersion < 1 || effectiveAt == null))
            throw new RouteDeviationException("INVALID_RULE_TRANSITION", "Active rule requires an effective version and time");
    }
    public boolean editable(){return lifecycle==Lifecycle.DRAFT||lifecycle==Lifecycle.DISABLED;}
    public RouteDeviationRule update(DistanceMeters tolerance){
        if(!editable())throw error("RULE_NOT_EDITABLE","Only draft or disabled rules are editable");
        return new RouteDeviationRule(id,tenantId,routeId,routeVersion,tolerance,lifecycle,ruleVersion,manageVersion+1,effectiveAt);
    }
    public RouteDeviationRule activate(Instant at){
        if(!editable())throw error("INVALID_RULE_TRANSITION","Only draft or disabled rules activate");
        return new RouteDeviationRule(id,tenantId,routeId,routeVersion,configuredTolerance,Lifecycle.ACTIVE,ruleVersion+1,manageVersion+1,Objects.requireNonNull(at));
    }
    public RouteDeviationRule disable(){
        if(lifecycle!=Lifecycle.ACTIVE)throw error("INVALID_RULE_TRANSITION","Only active rules disable");
        return new RouteDeviationRule(id,tenantId,routeId,routeVersion,configuredTolerance,Lifecycle.DISABLED,ruleVersion,manageVersion+1,effectiveAt);
    }
    public RouteDeviationRule retire(){
        if(lifecycle==Lifecycle.ACTIVE||lifecycle==Lifecycle.RETIRED)throw error("INVALID_RULE_TRANSITION","Disable active rule before retirement");
        return new RouteDeviationRule(id,tenantId,routeId,routeVersion,configuredTolerance,Lifecycle.RETIRED,ruleVersion,manageVersion+1,effectiveAt);
    }
    private static RouteDeviationException error(String c,String m){return new RouteDeviationException(c,m);}
}
