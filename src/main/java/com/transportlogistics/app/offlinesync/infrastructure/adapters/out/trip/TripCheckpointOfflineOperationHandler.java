package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;
import org.springframework.stereotype.Component;

@Component
final class TripCheckpointOfflineOperationHandler extends AbstractTripOperationalEventOfflineHandler {
    TripCheckpointOfflineOperationHandler(TripOperationalEventRecorder events) {
        super(events);
    }

    @Override
    public String operationType() {
        return "TRIP_CHECKPOINT_RECORD";
    }

    @Override
    public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
        var parsed = TripOperationalEventPayloadParser.checkpoint(payload);
        return invoke(() -> events.recordCheckpoint(new TripOperationalEventRecorder.CheckpointCommand(
                context.aggregateId(), parsed.checkpointType(), parsed.occurredAt(), parsed.locationId(),
                parsed.locationDescription(), parsed.remarks(), context.actorName())));
    }
}
