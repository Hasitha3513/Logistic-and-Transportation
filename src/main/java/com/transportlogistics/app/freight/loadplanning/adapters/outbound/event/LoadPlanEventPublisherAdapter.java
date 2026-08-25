package com.transportlogistics.app.freight.loadplanning.adapters.outbound.event;

import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanCreated;
import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanUpdated;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LoadPlanEventPublisherAdapter implements LoadPlanEventPublisher {

    private final ApplicationEventPublisher publisher;

    public LoadPlanEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishLoadPlanCreated(LoadPlanCreated event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishLoadPlanUpdated(LoadPlanUpdated event) {
        publisher.publishEvent(event);
    }
}
