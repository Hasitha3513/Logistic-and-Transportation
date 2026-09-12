package com.transportlogistics.app.tracking.ports.outbound;

import java.util.function.Supplier;

public interface SpeedManagementTransactionPort {
    <T> T execute(Supplier<T> work);
}
