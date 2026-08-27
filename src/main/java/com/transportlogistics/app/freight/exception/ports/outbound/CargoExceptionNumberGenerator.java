package com.transportlogistics.app.freight.exception.ports.outbound;

/**
 * Outbound port for generating human-readable exception reference numbers.
 */
public interface CargoExceptionNumberGenerator {

    /** Returns a new unique business reference, e.g. {@code CEX-2026-000001}. */
    String nextExceptionNumber();
}
