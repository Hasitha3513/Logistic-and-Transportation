package com.transportlogistics.app.freight.order.ports.outbound;

import java.util.function.Supplier;

public interface FreightOrderTransaction {
    <T> T execute(Supplier<T> operation);
}
