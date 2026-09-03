package com.transportlogistics.app.identity.infrastructure.security;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
class RequestTenantContext implements CurrentTenant, TenantContextExecutor {
    private final ThreadLocal<TenantExecutionContext> current = new ThreadLocal<>();

    void establish(TenantExecutionContext context) {
        if (current.get() != null) {
            throw new IllegalStateException("Tenant context is already established");
        }
        current.set(context);
    }

    void clear() {
        current.remove();
    }

    @Override
    public Optional<TenantExecutionContext> current() {
        return Optional.ofNullable(current.get());
    }

    @Override
    public <T> T within(TenantExecutionContext context, Supplier<T> work) {
        establish(context);
        try {
            return work.get();
        } finally {
            clear();
        }
    }
}
