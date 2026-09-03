package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionCase;

public interface DeliveryOperationalExceptionPublisher {
    void publish(DeliveryExceptionCase exceptionCase);
    static DeliveryOperationalExceptionPublisher noop() { return ignored -> { }; }
}
