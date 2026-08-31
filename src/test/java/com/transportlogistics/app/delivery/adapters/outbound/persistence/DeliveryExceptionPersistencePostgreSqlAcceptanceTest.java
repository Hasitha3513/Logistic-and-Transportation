package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DeliveryExceptionPersistenceAdapter.class})
class DeliveryExceptionPersistencePostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private DeliveryExceptionPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    private final OffsetDateTime now = OffsetDateTime.now();
    private UUID deliveryOrderIdA;
    private UUID deliveryOrderIdB;

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));

        deliveryOrderIdA = UUID.randomUUID();
        deliveryOrderIdB = UUID.randomUUID();
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID loc1 = UUID.fromString("22222222-2222-2222-2222-222222222221");
        UUID loc2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Insert sample delivery orders for Tenant A and Tenant B
        jdbcTemplate.update(
                "INSERT INTO delivery_order (id, tenant_id, delivery_number, customer_id, origin_location_id, " +
                "destination_location_id, priority, service_type, window_start, window_end, status, version, " +
                "created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deliveryOrderIdA, TENANT_A, "DEL-2026-900001", customerId, loc1, loc2,
                "NORMAL", "STANDARD", now.minusHours(1), now.plusHours(1), "READY_FOR_ASSIGNMENT", 0,
                now, now, "seed", "seed"
        );

        jdbcTemplate.update(
                "INSERT INTO delivery_order (id, tenant_id, delivery_number, customer_id, origin_location_id, " +
                "destination_location_id, priority, service_type, window_start, window_end, status, version, " +
                "created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deliveryOrderIdB, TENANT_B, "DEL-2026-900002", customerId, loc1, loc2,
                "NORMAL", "STANDARD", now.minusHours(1), now.plusHours(1), "READY_FOR_ASSIGNMENT", 0,
                now, now, "seed", "seed"
        );
    }

    @Test
    void persistAndRetrieveDamagedDeliveryWithEvidence() {
        UUID caseId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        var ev = new DeliveryExceptionEvidence(
                evId, caseId, "store/ref-1", "image/png", 1024, "checksum123", "damage.png", "driver.bob", now
        );

        var exc = DeliveryExceptionCase.create(
                caseId, new DeliveryId(deliveryOrderIdA), null, DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH, "Box damaged", null, null, null, null,
                null, null, List.of(ev), "driver.bob", now
        );

        DeliveryExceptionCase saved = adapter.save(exc);
        assertThat(saved.id()).isEqualTo(caseId);

        Optional<DeliveryExceptionCase> found = adapter.findById(caseId);
        assertThat(found).isPresent();
        assertThat(found.get().evidence()).hasSize(1);
        assertThat(found.get().evidence().get(0).detectedContentType()).isEqualTo("image/png");
    }

    @Test
    void partialUniqueIndexPreventsDuplicateActiveExceptionOfSameType() {
        UUID case1 = UUID.randomUUID();
        UUID case2 = UUID.randomUUID();

        var exc1 = DeliveryExceptionCase.create(
                case1, new DeliveryId(deliveryOrderIdA), null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM, "Attempt 1 wrong address", null, null, null, null,
                null, null, List.of(), "driver.bob", now
        );
        adapter.save(exc1);

        var exc2 = DeliveryExceptionCase.create(
                case2, new DeliveryId(deliveryOrderIdA), null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.HIGH, "Attempt 2 wrong address duplicate", null, null, null, null,
                null, null, List.of(), "driver.bob", now
        );

        assertThatThrownBy(() -> adapter.save(exc2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void crossTenantIsolationPreventsTenantBAccessingTenantA() {
        UUID caseId = UUID.randomUUID();
        var exc = DeliveryExceptionCase.create(
                caseId, new DeliveryId(deliveryOrderIdA), null, DeliveryExceptionType.RECIPIENT_REFUSAL,
                DeliveryExceptionSeverity.LOW, "Customer refused", null, null, null, null,
                null, null, List.of(), "driver.bob", now
        );
        adapter.save(exc);

        // Switch context to Tenant B
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_B, "UTC")));

        Optional<DeliveryExceptionCase> tenantBView = adapter.findById(caseId);
        assertThat(tenantBView).isEmpty();

        List<DeliveryExceptionCase> listB = adapter.findByDeliveryOrderId(new DeliveryId(deliveryOrderIdA));
        assertThat(listB).isEmpty();
    }
}
