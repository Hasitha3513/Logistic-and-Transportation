package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface RouteDeviationEvaluationTransactionPort {
    <T> T execute(Supplier<T> operation);
}
