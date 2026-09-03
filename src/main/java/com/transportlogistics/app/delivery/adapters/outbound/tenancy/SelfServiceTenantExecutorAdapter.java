package com.transportlogistics.app.delivery.adapters.outbound.tenancy;

import com.transportlogistics.app.delivery.ports.outbound.SelfServiceTenantExecutor;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class SelfServiceTenantExecutorAdapter implements SelfServiceTenantExecutor {
    private static final UUID CUSTOMER_ACTOR = UUID.nameUUIDFromBytes("delivery-self-service".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private final TenantContextExecutor executor;
    public SelfServiceTenantExecutorAdapter(TenantContextExecutor executor) { this.executor = executor; }
    @Override public <T> T within(UUID tenantId, Supplier<T> work) {
        return executor.within(new TenantExecutionContext(tenantId, CUSTOMER_ACTOR, "delivery-self-service", null), work);
    }
}
