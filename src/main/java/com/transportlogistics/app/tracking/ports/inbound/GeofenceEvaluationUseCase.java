package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationResult;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import java.time.Instant;
import java.util.List;

public interface GeofenceEvaluationUseCase {
    List<GeofenceEvaluationResult> evaluate(GeofencePosition position, Instant evaluatedAt);
}
