package com.transportlogistics.app.integration.ports.outbound;

import java.util.function.Supplier;

public interface IntegrationTransaction {
    <T> T execute(Supplier<T> operation);
    default void execute(Runnable operation) { execute(() -> { operation.run(); return null; }); }
}
