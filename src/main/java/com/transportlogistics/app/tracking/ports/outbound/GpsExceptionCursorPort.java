package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.UUID;

public interface GpsExceptionCursorPort {
    String encode(UUID tenantId, String binding, Instant timestamp, UUID id);
    Position decode(UUID tenantId, String binding, String cursor);
    record Position(Instant timestamp, UUID id) { }
}
