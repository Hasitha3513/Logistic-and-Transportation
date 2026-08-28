package com.transportlogistics.app.tenancy;

import java.util.function.Supplier;

public interface TenantContextExecutor {
    <T> T within(TenantExecutionContext context, Supplier<T> work);

    default void within(TenantExecutionContext context, Runnable work) {
        within(context, () -> {
            work.run();
            return null;
        });
    }
}
