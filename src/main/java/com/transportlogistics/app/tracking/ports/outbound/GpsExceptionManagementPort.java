package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase.Acknowledgement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface GpsExceptionManagementPort {
    void serialize(UUID tenantId, String idempotencyKey);
    Optional<Command> command(UUID tenantId, String idempotencyKey);
    void complete(UUID tenantId, String idempotencyKey, String fingerprint, UUID actorId,
            UUID episodeId, long expectedVersion, Acknowledgement response, Instant now);
    void audit(UUID tenantId, UUID actorId, UUID episodeId, String correlationId, Instant now);
    record Command(String fingerprint, UUID actorId, Acknowledgement response) { }
}
