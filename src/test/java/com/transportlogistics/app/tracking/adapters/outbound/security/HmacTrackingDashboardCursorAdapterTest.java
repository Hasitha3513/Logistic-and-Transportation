package com.transportlogistics.app.tracking.adapters.outbound.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardException;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardCursorPort.CursorState;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HmacTrackingDashboardCursorAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-16T10:00:00Z");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final HmacTrackingDashboardCursorAdapter adapter = new HmacTrackingDashboardCursorAdapter(
            new ObjectMapper().registerModule(new JavaTimeModule()), "01234567890123456789012345678901");

    @Test
    void roundTripsOnlyForTheBoundTenantFilterAndLifetime() {
        var filter = new DashboardFilter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, true);
        var state = new CursorState(TENANT, filter, UUID.randomUUID(), NOW, NOW.plusSeconds(300));
        String cursor = adapter.encode(state);

        assertThat(adapter.decode(TENANT, filter, cursor, NOW)).isEqualTo(state);
        assertThatThrownBy(() -> adapter.decode(UUID.randomUUID(), filter, cursor, NOW))
                .isInstanceOf(TrackingDashboardException.class);
        assertThatThrownBy(() -> adapter.decode(TENANT, filter, cursor, NOW.plusSeconds(301)))
                .isInstanceOf(TrackingDashboardException.class);
        assertThatThrownBy(() -> adapter.decode(TENANT, filter, cursor + "x", NOW))
                .isInstanceOf(TrackingDashboardException.class);
    }
}
