package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface TelemetryStreamPublisherPort {
    Publication publishDurably(
            String partitionKey,
            CanonicalTelemetryEvent event,
            String correlationId,
            Duration acknowledgementTimeout);

    default List<Publication> publishBatchDurably(
            List<PublicationRequest> requests, Duration acknowledgementTimeout) {
        List<Publication> publications = new ArrayList<>(requests.size());
        for (PublicationRequest request : requests) {
            publications.add(publishDurably(
                    request.partitionKey(),
                    request.event(),
                    request.correlationId(),
                    acknowledgementTimeout));
        }
        return List.copyOf(publications);
    }

    record PublicationRequest(
            String partitionKey, CanonicalTelemetryEvent event, String correlationId) {}

    record Publication(int partition, long offset) {}
}
