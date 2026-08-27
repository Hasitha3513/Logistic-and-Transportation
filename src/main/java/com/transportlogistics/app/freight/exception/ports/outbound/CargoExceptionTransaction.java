package com.transportlogistics.app.freight.exception.ports.outbound;

import java.util.function.Supplier;

/**
 * Outbound transaction port for CargoException use-case atomicity.
 */
public interface CargoExceptionTransaction {

    <T> T execute(Supplier<T> work);
}
