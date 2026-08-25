package com.transportlogistics.app.routing.application.ports.out;

import java.util.function.Supplier;

public interface RouteTransaction {
    <T> T execute(Supplier<T> operation);
}
