package com.transportlogistics.app.tracking.adapters.outbound.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HmacJourneyReplayCursorAdapterTest {
    private final HmacJourneyReplayCursorAdapter adapter = new HmacJourneyReplayCursorAdapter(
            new ObjectMapper().registerModule(new JavaTimeModule()), "a-secure-test-secret-that-is-long-enough-2026");

    @Test
    void roundTripsBoundStateWithoutExposingPlainIdentifiers() {
        UUID tenant = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        CursorState state = new CursorState(new CursorBinding(tenant, ReplaySelector.vehicle(UUID.randomUUID()),
                new TimeRange(now.minusSeconds(60), now), now),
                new CursorPosition(now.minusSeconds(1), UUID.randomUUID()), now.plusSeconds(900));
        String encoded = adapter.encode(state);
        assertThat(encoded).doesNotContain(tenant.toString());
        assertThat(adapter.decode(encoded)).isEqualTo(state);
    }

    @Test
    void rejectsTampering() {
        assertThatThrownBy(() -> adapter.decode("tampered.cursor"))
                .isInstanceOf(JourneyReplayException.class);
    }
}
