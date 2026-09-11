package com.transportlogistics.app.operations.ports.outbound;

import java.util.function.Supplier;

public interface OperationsTransaction {
    <T> T execute(Supplier<T> operation);
}
