package com.transportlogistics.app.delivery.ports.outbound;

import java.util.UUID;
import java.util.function.Supplier;

public interface SelfServiceTenantExecutor {
    <T> T within(UUID tenantId, Supplier<T> work);
}
