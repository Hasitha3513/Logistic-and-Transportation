package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import java.time.Duration;

public interface TelemetryStreamPublisherPort {
    Publication publishDurably(
            String partitionKey,
            CanonicalTelemetryEvent event,
            String correlationId,
            Duration acknowledgementTimeout);

    record Publication(int partition, long offset) {}
}
