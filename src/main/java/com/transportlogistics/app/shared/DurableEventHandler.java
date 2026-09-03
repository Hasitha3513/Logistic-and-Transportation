package com.transportlogistics.app.shared;

/** Consumer adapter for one durable event route. */
public interface DurableEventHandler {
    String consumerName();

    void handle(DurableEventEnvelope event);
}
