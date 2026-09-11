package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionHandoff;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
class DurableFuelExceptionHandoff implements FuelExceptionHandoff {
    private final DurableEventPublisher events;
    DurableFuelExceptionHandoff(DurableEventPublisher events){this.events=events;}
    @Override public void publish(FuelExceptionCase v, UUID eventId, String reason, String correlationId){events.publish(new OperationalExceptionFactV1(eventId,v.tenantId(),OperationalExceptionFactV1.SourceModule.FUEL,v.category().name(),v.id(),v.updatedAt(),OperationalExceptionFactV1.Severity.valueOf(v.impact().name()),category(v.category()),"FUEL_EXCEPTION_ESCALATED",Map.of("fuelExceptionId",v.id().toString(),"sourceType",v.sourceType(),"sourceId",v.sourceId().toString()),correlationId));}
    private static OperationalExceptionFactV1.Category category(FuelExceptionCase.Category value){return switch(value){case SUSPECTED_FUEL_LOSS,NEGATIVE_BUNKER_BALANCE->OperationalExceptionFactV1.Category.OPERATIONAL;case INCORRECT_READING->OperationalExceptionFactV1.Category.TECHNICAL;case SUDDEN_PRICE_CHANGE,EMERGENCY_REFUEL->OperationalExceptionFactV1.Category.FINANCIAL;case FUEL_CARD_POLICY_DEVIATION->OperationalExceptionFactV1.Category.SECURITY;};}
}
