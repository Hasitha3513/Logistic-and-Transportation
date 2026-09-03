package com.transportlogistics.app.freight.order.adapters.outbound.events;

import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderEventPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringFreightOrderEventPublisher implements FreightOrderEventPublisher {
    private final AfterCommitEventPublisher publisher;
    SpringFreightOrderEventPublisher(AfterCommitEventPublisher publisher) { this.publisher = publisher; }
    @Override public void publish(Object event) { publisher.publish(event); }
}
