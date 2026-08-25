package com.transportlogistics.app.routing.infrastructure.adapters.out.events;

import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringRouteEventPublisherAdapter implements RouteEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(Object event) {
        if (event != null) {
            publisher.publishEvent(event);
        }
    }
}
