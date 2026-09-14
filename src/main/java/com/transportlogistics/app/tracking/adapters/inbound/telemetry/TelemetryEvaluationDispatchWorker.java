package com.transportlogistics.app.tracking.adapters.inbound.telemetry;

import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationPosition;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePoint;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class TelemetryEvaluationDispatchWorker {
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000091");
    private static final int MAX_ATTEMPTS = 10;
    private final TelemetryEvaluationDispatchPort dispatches;
    private final HistoricalTelemetryLookupPort history;
    private final GeofenceEvaluationUseCase geofence;
    private final SpeedEvaluationUseCase speed;
    private final RouteDeviationEvaluationUseCase routeDeviation;
    private final TenantContextExecutor tenantContexts;
    private final MeterRegistry meters;
    private final Clock clock;
    private final String owner = "telemetry-dispatch-" + UUID.randomUUID();

    TelemetryEvaluationDispatchWorker(TelemetryEvaluationDispatchPort dispatches,
            HistoricalTelemetryLookupPort history, GeofenceEvaluationUseCase geofence,
            SpeedEvaluationUseCase speed, RouteDeviationEvaluationUseCase routeDeviation,
            TenantContextExecutor tenantContexts, MeterRegistry meters, Clock clock) {
        this.dispatches=dispatches;this.history=history;this.geofence=geofence;this.speed=speed;
        this.routeDeviation=routeDeviation;this.tenantContexts=tenantContexts;this.meters=meters;this.clock=clock;
    }

    @Scheduled(fixedDelayString = "${app.tracking.hybrid-storage.evaluation-delay:1000}")
    void tick() {
        Instant now=clock.instant();
        for (var job:dispatches.claim(owner,now,now.plusSeconds(30),100)) process(job);
    }

    private void process(TelemetryEvaluationDispatchPort.Dispatch job) {
        Instant now=clock.instant();
        try {
            HistoricalTelemetry fact=history.findExact(job.tenantId(),job.sourceTimestamp(),job.historyId())
                    .filter(value -> value.vehicleId().equals(job.vehicleId())
                            && value.dedupeIdentity().equals(job.dedupeIdentity()))
                    .orElseThrow(() -> new IllegalStateException("HISTORY_IDENTITY_NOT_FOUND"));
            tenantContexts.within(new TenantExecutionContext(job.tenantId(),SYSTEM_ACTOR,
                    "tracking-telemetry-evaluation",job.id().toString()),()->evaluate(job.evaluator(),fact,now));
            dispatches.complete(job.id(),owner,clock.instant());
            meters.counter("tracking.telemetry.evaluation.dispatch","result","completed",
                    "evaluator",job.evaluator().name()).increment();
        } catch (RuntimeException exception) {
            String code=exception instanceof IllegalStateException ? "HISTORY_IDENTITY_INVALID" : "EVALUATOR_FAILED";
            if(job.attempts()>=MAX_ATTEMPTS) dispatches.fail(job.id(),owner,clock.instant(),code);
            else dispatches.retry(job.id(),owner,clock.instant(),clock.instant().plus(backoff(job.attempts())),code);
            meters.counter("tracking.telemetry.evaluation.dispatch","result","retry",
                    "evaluator",job.evaluator().name()).increment();
        }
    }

    private void evaluate(TelemetryEvaluationDispatchPort.Evaluator evaluator,
            HistoricalTelemetry fact, Instant now) {
        switch(evaluator) {
            case GEOFENCE -> geofence.evaluate(new GeofencePosition(fact.tenantId(),fact.eventId(),
                    fact.vehicleId(),fact.recordedAt(),new Wgs84Coordinate(
                            fact.longitude().doubleValue(),fact.latitude().doubleValue()),
                    fact.trust(),fact.ordering(),false),now);
            case SPEED -> speed.evaluate(new SpeedPosition(fact.tenantId(),fact.vehicleId(),fact.eventId(),
                    fact.recordedAt(),fact.speedKph()==null?null:new SpeedKph(fact.speedKph()),false,
                    fact.trust()==com.transportlogistics.app.tracking.domain.TrackingModels.Trust.TRUSTED,
                    true,fact.ordering()==com.transportlogistics.app.tracking.domain.TrackingModels.Ordering.IN_ORDER),now);
            case ROUTE_DEVIATION -> routeDeviation.evaluate(new RouteDeviationPosition(fact.eventId(),
                    fact.tenantId(),fact.vehicleId(),fact.recordedAt(),new RoutePoint(
                            fact.longitude(),fact.latitude()),
                    fact.horizontalAccuracyMeters()==null?null:DistanceMeters.of(
                            fact.horizontalAccuracyMeters().doubleValue()),
                    true,false,RouteDeviationPosition.Trust.valueOf(fact.trust().name()),true,
                    RouteDeviationPosition.Ordering.valueOf(fact.ordering().name())));
        }
    }

    private static Duration backoff(int attempts) { return Duration.ofSeconds(Math.min(300,5L<<Math.min(attempts,5))); }
}
