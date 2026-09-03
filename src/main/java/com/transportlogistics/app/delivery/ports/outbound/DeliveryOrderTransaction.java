package com.transportlogistics.app.delivery.ports.outbound;

import java.util.function.Supplier;

public interface DeliveryOrderTransaction {
    <T> T execute(Supplier<T> operation);
}
