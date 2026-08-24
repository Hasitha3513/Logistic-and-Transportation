package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;
import org.springframework.stereotype.Component;

@Component
final class TripIncidentOfflineOperationHandler extends AbstractTripOperationalEventOfflineHandler {
    TripIncidentOfflineOperationHandler(TripOperationalEventRecorder events) {
        super(events);
    }

    @Override
    public String operationType() {
        return "TRIP_INCIDENT_RECORD";
    }

    @Override
    public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
        var parsed = TripOperationalEventPayloadParser.incident(payload);
        return invoke(() -> events.recordIncident(new TripOperationalEventRecorder.IncidentCommand(
                context.aggregateId(), parsed.incidentSeverity(), parsed.description(), parsed.occurredAt(),
                parsed.locationId(), parsed.locationDescription(), parsed.remarks(), context.actorName())));
    }
}
