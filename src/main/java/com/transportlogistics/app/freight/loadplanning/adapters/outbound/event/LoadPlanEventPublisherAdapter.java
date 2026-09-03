package com.transportlogistics.app.freight.loadplanning.adapters.outbound.event;

import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanCreated;
import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanUpdated;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanEventPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LoadPlanEventPublisherAdapter implements LoadPlanEventPublisher {

    private final AfterCommitEventPublisher publisher;

    public LoadPlanEventPublisherAdapter(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishLoadPlanCreated(LoadPlanCreated event) {
        publisher.publish(event);
    }

    @Override
    public void publishLoadPlanUpdated(LoadPlanUpdated event) {
        publisher.publish(event);
    }
}
