package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EvaluationContext;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Observation;

public interface GpsReliabilityEvaluationUseCase {
    Assessment evaluate(Observation observation, EvaluationContext context);
}
