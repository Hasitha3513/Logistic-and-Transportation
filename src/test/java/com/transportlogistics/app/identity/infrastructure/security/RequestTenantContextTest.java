package com.transportlogistics.app.identity.infrastructure.security;

import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestTenantContextTest {
    @Test
    void clearingRequestPreventsCrossRequestLeakage() {
        var context = new RequestTenantContext();
        var tenantA = new TenantExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "a", "request-a");
        var tenantB = new TenantExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "b", "request-b");

        context.establish(tenantA);
        assertThat(context.required()).isEqualTo(tenantA);
        context.clear();
        assertThat(context.current()).isEmpty();

        context.establish(tenantB);
        assertThat(context.required()).isEqualTo(tenantB);
        context.clear();
        assertThat(context.current()).isEmpty();
    }

    @Test
    void exceptionPathClearsRequestContext() {
        var context = new RequestTenantContext();
        var tenant = new TenantExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "operator", "request-error");

        assertThatThrownBy(() -> {
            try {
                context.establish(tenant);
                throw new IllegalStateException("downstream failure");
            } finally {
                context.clear();
            }
        }).isInstanceOf(IllegalStateException.class);

        assertThat(context.current()).isEmpty();
    }
}
