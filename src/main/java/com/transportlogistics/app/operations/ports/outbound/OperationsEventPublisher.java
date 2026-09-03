package com.transportlogistics.app.operations.ports.outbound;

import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;

import java.time.OffsetDateTime;

public interface OperationsEventPublisher {
    void publishEscalation(OperationalExceptionCase exceptionCase, OffsetDateTime occurredAt);
}
