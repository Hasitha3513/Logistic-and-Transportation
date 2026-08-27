package com.transportlogistics.app.fuel.application.ports.out;

import java.util.function.Supplier;

public interface FuelTransaction {
    <T> T execute(Supplier<T> operation);
}
