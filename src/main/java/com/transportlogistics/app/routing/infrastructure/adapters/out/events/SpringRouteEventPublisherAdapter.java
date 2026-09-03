package com.transportlogistics.app.routing.infrastructure.adapters.out.events;

import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringRouteEventPublisherAdapter implements RouteEventPublisher {
    private final AfterCommitEventPublisher publisher;

    @Override
    public void publish(Object event) {
        if (event != null) {
            publisher.publish(event);
        }
    }
}
