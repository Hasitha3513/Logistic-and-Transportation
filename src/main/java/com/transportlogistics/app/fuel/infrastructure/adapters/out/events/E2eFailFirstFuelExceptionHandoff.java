package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionHandoff;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** E2E-only adapter-boundary failure injection for durable handoff retry proof. */
@Component
@Primary
@Profile("e2e")
class E2eFailFirstFuelExceptionHandoff implements FuelExceptionHandoff {
    static final String FAILURE_MARKER = "[E2E_FAIL_FIRST]";
    private final DurableFuelExceptionHandoff delegate;
    private final Set<UUID> failedEventIds = ConcurrentHashMap.newKeySet();

    E2eFailFirstFuelExceptionHandoff(DurableFuelExceptionHandoff delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(FuelExceptionCase value, UUID eventId, String reason, String correlationId) {
        if (reason != null && reason.startsWith(FAILURE_MARKER) && failedEventIds.add(eventId)) {
            throw new IllegalStateException("Controlled first-attempt Fuel handoff failure");
        }
        delegate.publish(value, eventId, reason, correlationId);
    }
}
