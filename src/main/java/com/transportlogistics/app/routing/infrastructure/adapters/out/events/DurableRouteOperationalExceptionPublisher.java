package com.transportlogistics.app.routing.infrastructure.adapters.out.events;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.routing.application.ports.out.RouteOperationalExceptionPublisher;
import com.transportlogistics.app.routing.domain.model.RouteDisruption;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class DurableRouteOperationalExceptionPublisher implements RouteOperationalExceptionPublisher {
    private final DurableEventPublisher events;
    private final CurrentTenant currentTenant;

    DurableRouteOperationalExceptionPublisher(DurableEventPublisher events, CurrentTenant currentTenant) {
        this.events = events;
        this.currentTenant = currentTenant;
    }

    @Override
    public void publish(RouteDisruption disruption) {
        var context = currentTenant.required();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("routeId", disruption.routeId().toString());
        if (disruption.detourRouteId() != null) metadata.put("detourRouteId", disruption.detourRouteId().toString());
        metadata.put("effectiveFrom", disruption.effectiveFrom().toString());
        if (disruption.effectiveUntil() != null) metadata.put("effectiveUntil", disruption.effectiveUntil().toString());
        events.publish(new OperationalExceptionFactV1(disruption.id(), context.tenantId(),
            OperationalExceptionFactV1.SourceModule.ROUTING, disruption.disruptionType().name(), disruption.id(),
            disruption.createdAt(), OperationalExceptionFactV1.Severity.valueOf(disruption.severity().name()),
            category(disruption), "ROUTE_DISRUPTION_CREATED", Map.copyOf(metadata), context.correlationId()));
    }

    private static OperationalExceptionFactV1.Category category(RouteDisruption disruption) {
        return switch (disruption.disruptionType()) {
            case ACCIDENT -> OperationalExceptionFactV1.Category.SAFETY;
            case RESTRICTION -> OperationalExceptionFactV1.Category.COMPLIANCE;
            case ROAD_CLOSURE, WEATHER -> OperationalExceptionFactV1.Category.OPERATIONAL;
        };
    }
}
