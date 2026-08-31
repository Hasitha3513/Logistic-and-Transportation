package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
@Import({DeliveryExceptionPersistenceAdapter.class})
class DeliveryExceptionConcurrencyPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private DeliveryExceptionPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    private final OffsetDateTime now = OffsetDateTime.now();
    private UUID deliveryOrderId;

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));

        deliveryOrderId = UUID.randomUUID();
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID loc1 = UUID.fromString("22222222-2222-2222-2222-222222222221");
        UUID loc2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        jdbcTemplate.update(
                "INSERT INTO delivery_order (id, tenant_id, delivery_number, customer_id, origin_location_id, " +
                "destination_location_id, priority, service_type, window_start, window_end, status, version, " +
                "created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deliveryOrderId, TENANT_A, "DEL-CONC-" + (int)(Math.random()*9000 + 1000),
                customerId, loc1, loc2, "NORMAL", "STANDARD", now.minusHours(1), now.plusHours(1),
                "READY_FOR_ASSIGNMENT", 0, now, now, "seed", "seed"
        );
    }

    @Test
    @DisplayName("Concurrently creating 2 active exceptions of the exact same type for the same delivery results in exactly 1 winner and 1 constraint violation")
    void duplicateActiveCreationRaceCondition() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger constraintFailures = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                UUID caseId = UUID.randomUUID();
                var exc = DeliveryExceptionCase.create(
                        caseId, new DeliveryId(deliveryOrderId), null, DeliveryExceptionType.RECIPIENT_REFUSAL,
                        DeliveryExceptionSeverity.MEDIUM, "Attempt concurrent", null, null, null, null,
                        null, null, List.of(), "driver.concurrency", now
                );
                try {
                    barrier.await();
                    adapter.save(exc);
                    successes.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    constraintFailures.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof DataIntegrityViolationException) {
                        constraintFailures.incrementAndGet();
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(constraintFailures.get()).isEqualTo(1);

        List<DeliveryExceptionCase> found = adapter.findByDeliveryOrderId(new DeliveryId(deliveryOrderId));
        assertThat(found).hasSize(1);
    }
}
