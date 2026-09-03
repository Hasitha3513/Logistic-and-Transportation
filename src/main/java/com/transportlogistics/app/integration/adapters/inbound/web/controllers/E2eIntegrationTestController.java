package com.transportlogistics.app.integration.adapters.inbound.web.controllers;

import com.transportlogistics.app.integration.IntegrationPlatformProbeEvent;
import com.transportlogistics.app.integration.ports.inbound.IntegrationExchangeUseCase;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Profile-restricted deterministic controls for the isolated real E2E runtime. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/integrations")
public class E2eIntegrationTestController {
    private final DurableEventPublisher events;
    private final IntegrationExchangeUseCase exchanges;
    private final CurrentTenant currentTenant;

    public E2eIntegrationTestController(DurableEventPublisher events, IntegrationExchangeUseCase exchanges,
                                        CurrentTenant currentTenant) {
        this.events = events;
        this.exchanges = exchanges;
        this.currentTenant = currentTenant;
    }

    @PostMapping("/{configurationId}/replay")
    @Transactional
    void replay(@org.springframework.web.bind.annotation.PathVariable UUID configurationId,
                @RequestBody ReplayRequest request) {
        var tenant = currentTenant.required();
        events.publish(new IntegrationPlatformProbeEvent(request.eventId(), tenant.tenantId(), configurationId,
            request.probeId(), request.sequence(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @PostMapping("/process")
    void process() {
        exchanges.processDue(currentTenant.required().tenantId());
    }

    record ReplayRequest(UUID eventId, UUID probeId, long sequence) {}
}
