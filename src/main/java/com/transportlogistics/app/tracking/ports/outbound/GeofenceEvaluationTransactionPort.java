package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface GeofenceEvaluationTransactionPort {
    <T> T execute(Supplier<T> operation);
}
