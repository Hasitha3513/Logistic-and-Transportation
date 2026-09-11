package com.transportlogistics.app.fuel.domain.model;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelCardTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-04T10:00:00Z");

    @Test void allowsOnlyFrozenLifecycleTransitions() {
        FuelCard draft = card(FuelCard.Status.DRAFT, 12, 2027);
        FuelCard active = draft.transition(FuelCard.Status.ACTIVE, NOW, ZoneId.of("UTC"));
        assertThat(active.status()).isEqualTo(FuelCard.Status.ACTIVE);
        assertThat(active.transition(FuelCard.Status.SUSPENDED, NOW, ZoneId.of("UTC")).status())
                .isEqualTo(FuelCard.Status.SUSPENDED);
        assertThatThrownBy(() -> active.transition(FuelCard.Status.CANCELLED, NOW, ZoneId.of("UTC")))
                .hasMessage("FUEL_CARD_INVALID_STATE");
    }

    @Test void derivesExpiryUsingTenantTimezoneAndNeverReactivates() {
        FuelCard expired = card(FuelCard.Status.SUSPENDED, 8, 2026);
        assertThat(expired.effectiveStatus(NOW, ZoneId.of("Asia/Colombo")))
                .isEqualTo(FuelCard.Status.EXPIRED);
        assertThatThrownBy(() -> expired.transition(FuelCard.Status.ACTIVE, NOW, ZoneId.of("Asia/Colombo")))
                .hasMessage("FUEL_CARD_EXPIRED");
    }

    private FuelCard card(FuelCard.Status status, int month, int year) {
        return new FuelCard(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Operations card",
                "opaque-provider-reference", "**** 4242", "4242", month, year, status, 0,
                UUID.randomUUID(), NOW.minusDays(1), NOW.minusDays(1));
    }
}
