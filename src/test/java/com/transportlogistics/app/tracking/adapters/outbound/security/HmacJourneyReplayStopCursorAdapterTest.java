package com.transportlogistics.app.tracking.adapters.outbound.security;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HmacJourneyReplayStopCursorAdapterTest {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String secret = "a-secure-test-secret-that-is-long-enough-2026";
    private final HmacJourneyReplayStopCursorAdapter stops =
            new HmacJourneyReplayStopCursorAdapter(json, secret);
    private final HmacJourneyReplayCursorAdapter points = new HmacJourneyReplayCursorAdapter(json, secret);

    @Test void roundTripsPurposeBoundStateAndRejectsPointCursor() {
        StopCursorState state = state();
        String encoded = stops.encode(state);
        assertThat(encoded).startsWith("STOP.").doesNotContain(state.tenantId().toString());
        assertThat(stops.decode(encoded)).isEqualTo(state);
        assertThatThrownBy(() -> points.decode(encoded)).isInstanceOf(JourneyReplayException.class);
    }

    @Test void pointCursorIsRejectedAsStopCursor() {
        StopCursorState stop = state();
        CursorState point = new CursorState(new CursorBinding(stop.tenantId(), stop.selector(),
                stop.requestedRange(), stop.snapshotRecordedAt()),
                new CursorPosition(stop.lastStartSourceTimestamp(), UUID.randomUUID()), stop.expiresAt());
        assertThatThrownBy(() -> stops.decode(points.encode(point)))
                .isInstanceOf(JourneyReplayException.class);
    }

    private static StopCursorState state() {
        Instant now = Instant.parse("2026-09-15T00:00:00Z");
        TimeRange range = new TimeRange(now.minusSeconds(600), now);
        return new StopCursorState(UUID.randomUUID(), ReplaySelector.vehicle(UUID.randomUUID()), range,
                range, now, now.minusSeconds(60), "digest", STOP_RULE_VERSION, now.plusSeconds(900));
    }
}
