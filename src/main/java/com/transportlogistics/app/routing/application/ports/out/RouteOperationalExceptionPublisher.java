package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.domain.model.RouteDisruption;

public interface RouteOperationalExceptionPublisher {
    void publish(RouteDisruption disruption);

    static RouteOperationalExceptionPublisher noop() { return ignored -> { }; }
}
