package com.transportlogistics.app.fleet.application.ports.out;

import java.util.function.Supplier;

public interface VehicleReadingTransaction {
    <T> T execute(Supplier<T> operation);
}
