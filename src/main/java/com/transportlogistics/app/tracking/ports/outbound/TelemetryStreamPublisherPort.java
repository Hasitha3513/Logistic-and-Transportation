package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import java.time.Duration;

public interface TelemetryStreamPublisherPort {
    Publication publishDurably(
            String partitionKey,
            TrackingTelemetryIngestedV1 event,
            String correlationId,
            Duration acknowledgementTimeout);

    record Publication(int partition, long offset) {}
}
