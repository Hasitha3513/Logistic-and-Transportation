package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface SpeedEvaluationTransactionPort {
    <T> T execute(Supplier<T> operation);
}
