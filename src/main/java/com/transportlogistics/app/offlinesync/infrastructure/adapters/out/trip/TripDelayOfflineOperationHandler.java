package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;
import org.springframework.stereotype.Component;

@Component
final class TripDelayOfflineOperationHandler extends AbstractTripOperationalEventOfflineHandler {
    TripDelayOfflineOperationHandler(TripOperationalEventRecorder events) {
        super(events);
    }

    @Override
    public String operationType() {
        return "TRIP_DELAY_RECORD";
    }

    @Override
    public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
        var parsed = TripOperationalEventPayloadParser.delay(payload);
        return invoke(() -> events.recordDelay(new TripOperationalEventRecorder.DelayCommand(
                context.aggregateId(), parsed.delayMinutes(), parsed.reason(), parsed.occurredAt(),
                parsed.locationId(), parsed.locationDescription(), parsed.remarks(), context.actorName())));
    }
}
