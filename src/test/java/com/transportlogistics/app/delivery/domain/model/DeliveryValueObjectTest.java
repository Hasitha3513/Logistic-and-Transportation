package com.transportlogistics.app.delivery.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryValueObjectTest {
    @Test
    void deliveryIdRequiresValue() {
        UUID value = UUID.randomUUID();

        assertThat(new DeliveryId(value).value()).isEqualTo(value);
        assertThatThrownBy(() -> new DeliveryId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Delivery id is required");
    }

    @Test
    void deliveryNumberIsTrimmedAndBounded() {
        assertThat(new DeliveryNumber(" DEL-2026-000001 ").value()).isEqualTo("DEL-2026-000001");
        assertThatThrownBy(() -> new DeliveryNumber(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Delivery number must match DEL-YYYY-NNNNNN");
        assertThatThrownBy(() -> new DeliveryNumber("D".repeat(41)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Delivery number must match DEL-YYYY-NNNNNN");
    }
}
