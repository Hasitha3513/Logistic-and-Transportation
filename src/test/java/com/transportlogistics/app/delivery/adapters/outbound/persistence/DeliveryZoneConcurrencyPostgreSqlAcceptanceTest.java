package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({DeliveryZonePersistenceAdapter.class, ObjectMapper.class})
class DeliveryZoneConcurrencyPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private DeliveryZonePersistenceAdapter adapter;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));
    }

    @Test
    @DisplayName("Concurrently creating 2 zones with identical zone_code results in exactly 1 winner and 1 constraint violation")
    void duplicateZoneCodeRaceCondition() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        String duplicateCode = "ZONE-RACE-" + (int)(Math.random() * 9000 + 1000);
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(0.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 0.0)
        );

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            tasks.add(() -> {
                barrier.await();
                try {
                    DeliveryZone zone = DeliveryZone.create(
                            TENANT_A,
                            duplicateCode,
                            "Race Zone " + index,
                            null,
                            DeliveryZoneType.URBAN_DENSE,
                            true,
                            null,
                            null,
                            new DeliveryZoneBoundary(coords),
                            0,
                            "admin",
                            now
                    );
                    adapter.save(zone);
                    successes.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    failures.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                        failures.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                return null;
            });
        }

        executor.invokeAll(tasks);
        executor.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
    }
}
