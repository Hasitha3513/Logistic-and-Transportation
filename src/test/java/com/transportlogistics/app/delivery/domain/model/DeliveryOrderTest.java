package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DeliveryOrderTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-29T10:00:00+05:30");
    @Test void appliesFrozenDefaultsAndStartsDraft() {
        var order = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber("DEL-2026-000001"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null, new DeliveryWindow(now, now.plusHours(2)), " note ", now, "user");
        assertThat(order.priority()).isEqualTo(DeliveryPriority.NORMAL); assertThat(order.serviceType()).isEqualTo(DeliveryServiceType.STANDARD); assertThat(order.status()).isEqualTo(DeliveryStatus.DRAFT); assertThat(order.instructions()).isEqualTo("note");
    }
    @Test void exposesOnlyTheUs56LifecycleStates() {
        assertThat(List.of(DeliveryStatus.values()))
                .containsExactly(DeliveryStatus.DRAFT, DeliveryStatus.READY_FOR_ASSIGNMENT);
    }
    @Test void materialEditReturnsReadyOrderToDraftAndKeepsNumberImmutable() {
        var number = new DeliveryNumber("DEL-2026-000001"); var origin = UUID.randomUUID();
        var ready = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), number, UUID.randomUUID(), origin, UUID.randomUUID(), DeliveryPriority.HIGH, DeliveryServiceType.EXPRESS, new DeliveryWindow(now, now.plusHours(2)), null, now, "user").markReadyForAssignment(now.plusMinutes(1), "user");
        var updated = ready.updateRequirements(ready.customerId(), origin, ready.destinationLocationId(), DeliveryPriority.URGENT, ready.serviceType(), ready.window(), null, now.plusMinutes(2), "editor");
        assertThat(updated.status()).isEqualTo(DeliveryStatus.DRAFT); assertThat(updated.deliveryNumber()).isEqualTo(number);
    }
    @Test void readinessTransitionCannotBeRepeated() {
        var order = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber("DEL-2026-000001"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null, new DeliveryWindow(now, now.plusHours(2)), null, now, "user").markReadyForAssignment(now, "user");
        assertThatThrownBy(() -> order.markReadyForAssignment(now, "user")).isInstanceOf(BusinessRuleException.class);
    }
}
