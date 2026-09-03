package com.transportlogistics.app.integration.adapters.inbound.events;

import com.transportlogistics.app.integration.IntegrationPlatformProbeEvent;
import com.transportlogistics.app.integration.ports.inbound.IntegrationExchangeUseCase;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IntegrationDurableEventHandler implements DurableEventHandler {
    private final IntegrationExchangeUseCase exchanges;

    public IntegrationDurableEventHandler(IntegrationExchangeUseCase exchanges) { this.exchanges = exchanges; }

    @Override public String consumerName() { return IntegrationPlatformProbeEvent.CONSUMER; }

    @Override
    @Transactional
    public void handle(DurableEventEnvelope event) {
        try {
            exchanges.acceptProbe(new IntegrationExchangeUseCase.ProbeFact(event.eventId(), event.tenantId(),
                event.aggregateId(), event.eventType(), event.version(), event.aggregateType(), event.occurredAt(),
                event.payload()));
        } catch (IllegalArgumentException exception) {
            String code = exception instanceof BusinessRuleException business ? business.code()
                : "INTEGRATION_PAYLOAD_INVALID";
            throw new PermanentEventFailureException(code, exception.getMessage());
        }
    }
}
