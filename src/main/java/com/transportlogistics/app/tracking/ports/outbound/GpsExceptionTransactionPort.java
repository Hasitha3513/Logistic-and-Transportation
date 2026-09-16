package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface GpsExceptionTransactionPort {
    <T> T execute(Supplier<T> operation);
}
