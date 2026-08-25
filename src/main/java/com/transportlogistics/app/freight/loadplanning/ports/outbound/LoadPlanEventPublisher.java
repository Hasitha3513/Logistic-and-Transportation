package com.transportlogistics.app.freight.loadplanning.ports.outbound;

import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanCreated;
import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanUpdated;

/**
 * Outbound port for publishing LoadPlan domain events.
 */
public interface LoadPlanEventPublisher {

    void publishLoadPlanCreated(LoadPlanCreated event);

    void publishLoadPlanUpdated(LoadPlanUpdated event);
}
