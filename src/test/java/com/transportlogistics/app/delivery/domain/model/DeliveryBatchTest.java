package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryBatchTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID riderId = UUID.randomUUID();
    private final DeliveryBatchCode batchCode = new DeliveryBatchCode("BAT-2026-000001");
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Should create valid DeliveryBatch with DRAFT status")
    void createDeliveryBatch_valid_succeeds() {
        DeliveryBatch batch = DeliveryBatch.create(
                UUID.randomUUID(),
                tenantId,
                batchCode,
                zoneId,
                slotId,
                5,
                now,
                "dispatcher"
        );

        assertThat(batch.tenantId()).isEqualTo(tenantId);
        assertThat(batch.batchCode().value()).isEqualTo("BAT-2026-000001");
        assertThat(batch.deliveryZoneId()).isEqualTo(zoneId);
        assertThat(batch.deliverySlotId()).isEqualTo(slotId);
        assertThat(batch.status()).isEqualTo(DeliveryBatchStatus.DRAFT);
        assertThat(batch.maxBatchSize()).isEqualTo(5);
        assertThat(batch.riderId()).isNull();
    }

    @Test
    @DisplayName("Should validate DeliveryBatch mandatory fields")
    void createDeliveryBatch_invalidFields_throws() {
        assertThatThrownBy(() -> new DeliveryBatch(
                UUID.randomUUID(),
                tenantId,
                null,
                zoneId,
                slotId,
                null,
                DeliveryBatchStatus.DRAFT,
                5,
                0L,
                now,
                now,
                "admin",
                "admin"
        )).isInstanceOf(BusinessRuleException.class);

        assertThatThrownBy(() -> DeliveryBatch.create(
                UUID.randomUUID(),
                tenantId,
                batchCode,
                zoneId,
                slotId,
                0,
                now,
                "admin"
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Should enforce lifecycle transitions: DRAFT -> READY -> ASSIGNED -> DISPATCHED -> COMPLETED")
    void batchLifecycle_validTransitions_succeed() {
        DeliveryBatch batch = DeliveryBatch.create(UUID.randomUUID(), tenantId, batchCode, zoneId, slotId, 5, now, "admin");

        // Mark READY
        DeliveryBatch readyBatch = batch.markReady(3, now.plusMinutes(5), "admin");
        assertThat(readyBatch.status()).isEqualTo(DeliveryBatchStatus.READY);

        // Assign Rider
        DeliveryBatch assignedBatch = readyBatch.assignRider(riderId, now.plusMinutes(10), "admin");
        assertThat(assignedBatch.status()).isEqualTo(DeliveryBatchStatus.ASSIGNED);
        assertThat(assignedBatch.riderId()).isEqualTo(riderId);

        // Dispatch
        DeliveryBatch dispatchedBatch = assignedBatch.dispatch(now.plusMinutes(15), "admin");
        assertThat(dispatchedBatch.status()).isEqualTo(DeliveryBatchStatus.DISPATCHED);

        // Complete
        DeliveryBatch completedBatch = dispatchedBatch.complete(now.plusMinutes(30), "admin");
        assertThat(completedBatch.status()).isEqualTo(DeliveryBatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should reject invalid lifecycle transitions")
    void batchLifecycle_invalidTransitions_throw() {
        DeliveryBatch batch = DeliveryBatch.create(UUID.randomUUID(), tenantId, batchCode, zoneId, slotId, 5, now, "admin");

        // Cannot mark ready if empty
        assertThatThrownBy(() -> batch.markReady(0, now, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot mark an empty batch as READY");

        // Cannot dispatch from DRAFT
        assertThatThrownBy(() -> batch.dispatch(now, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Batch must be in ASSIGNED status to dispatch");

        // Cannot complete from DRAFT
        assertThatThrownBy(() -> batch.complete(now, "admin"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Should cancel batch and prevent modifications on cancelled/completed batch")
    void batchCancellation_succeeds_and_prevents_updates() {
        DeliveryBatch batch = DeliveryBatch.create(UUID.randomUUID(), tenantId, batchCode, zoneId, slotId, 5, now, "admin");
        DeliveryBatch cancelled = batch.cancel(now, "admin");

        assertThat(cancelled.status()).isEqualTo(DeliveryBatchStatus.CANCELLED);

        assertThatThrownBy(() -> cancelled.cancel(now, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be cancelled");

        assertThatThrownBy(() -> cancelled.updateMetadata(10, now, "admin"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
