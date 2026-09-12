package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import java.util.Optional;
import java.util.UUID;

public interface SpeedPositionRepositoryPort {
    Optional<SpeedPosition> find(UUID tenantId, UUID positionId);
}
