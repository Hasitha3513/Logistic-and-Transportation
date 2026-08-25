package com.transportlogistics.app.freight.loadplanning.ports.outbound;

import java.util.function.Supplier;

public interface LoadPlanTransaction {
    <T> T execute(Supplier<T> operation);
}
