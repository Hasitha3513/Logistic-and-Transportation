package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface RouteDeviationManagementTransactionPort {
    <T> T execute(Supplier<T> work);
}
