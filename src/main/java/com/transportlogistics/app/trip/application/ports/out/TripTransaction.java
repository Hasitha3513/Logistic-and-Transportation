package com.transportlogistics.app.trip.application.ports.out;

import java.util.function.Supplier;

public interface TripTransaction {
    <T> T execute(Supplier<T> operation);
}
