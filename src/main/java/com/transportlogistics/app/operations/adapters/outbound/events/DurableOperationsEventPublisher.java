package com.transportlogistics.app.operations.adapters.outbound.events;

import com.transportlogistics.app.operations.OperationalExceptionEscalatedV1;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.ports.outbound.OperationsEventPublisher;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class DurableOperationsEventPublisher implements OperationsEventPublisher {
    private final DurableEventPublisher publisher;

    DurableOperationsEventPublisher(DurableEventPublisher publisher) { this.publisher = publisher; }

    @Override
    public void publishEscalation(OperationalExceptionCase value, OffsetDateTime occurredAt) {
        publisher.publish(new OperationalExceptionEscalatedV1(UUID.randomUUID(), value.tenantId(), value.id(),
            value.caseReference(), value.sourceModule().name(), value.sourceType(), value.sourceId(),
            value.category().name(), value.severity().name(), value.escalationLevel().name(),
            value.slaStatus(occurredAt).name(), occurredAt, value.correlationId()));
    }
}
