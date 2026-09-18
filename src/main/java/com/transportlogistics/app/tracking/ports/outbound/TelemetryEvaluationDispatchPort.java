package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TelemetryEvaluationDispatchPort {
    void enqueue(HistoricalTelemetry telemetry);
    List<Dispatch> claim(String owner, Instant now, Instant leaseUntil, int limit);
    List<Dispatch> claimIdle(String owner, Instant now, Instant leaseUntil, int limit);
    void complete(UUID dispatchId, String owner, Instant now);
    void retry(UUID dispatchId, String owner, Instant now, Instant nextAttemptAt, String errorCode);
    void fail(UUID dispatchId, String owner, Instant now, String errorCode);

    enum Evaluator { GEOFENCE, SPEED, ROUTE_DEVIATION, IDLE }
    record Dispatch(UUID id, UUID tenantId, Instant sourceTimestamp, UUID historyId,
                    String dedupeIdentity, UUID vehicleId, Evaluator evaluator, int attempts) { }
}
