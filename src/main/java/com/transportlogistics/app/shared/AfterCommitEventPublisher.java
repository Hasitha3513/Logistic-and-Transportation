package com.transportlogistics.app.shared;

/** Public shared technical contract for transaction-aware local event publication. */
public interface AfterCommitEventPublisher {
    void publish(Object event);
}
