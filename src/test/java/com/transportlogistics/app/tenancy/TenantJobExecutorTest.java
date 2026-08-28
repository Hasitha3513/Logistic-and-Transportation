package com.transportlogistics.app.tenancy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantJobExecutorTest {
    @Test
    void establishesDistinctContextForEveryActiveTenant() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TenantDirectory directory = new TenantDirectory() {
            @Override public Optional<TenantView> findTenant(UUID tenantId) { return Optional.empty(); }
            @Override public List<TenantView> findActiveTenants() {
                return List.of(view(first), view(second));
            }
        };
        var current = new ThreadLocal<TenantExecutionContext>();
        TenantContextExecutor contexts = new TenantContextExecutor() {
            @Override public <T> T within(TenantExecutionContext context, java.util.function.Supplier<T> work) {
                current.set(context);
                try { return work.get(); } finally { current.remove(); }
            }
        };
        var observed = new ArrayList<UUID>();

        new TenantJobExecutor(directory, contexts).forEachActiveTenant("test-job",
                () -> observed.add(current.get().tenantId()));

        assertEquals(List.of(first, second), observed);
    }

    private TenantDirectory.TenantView view(UUID id) {
        return new TenantDirectory.TenantView(id, "T", "Tenant", "LKR", "Asia/Colombo", "ACTIVE");
    }
}
