package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Mutation;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.PersistResult;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.State;
import java.util.Optional;
import java.util.UUID;

public interface IdlePersistencePort {
    Optional<State> findState(UUID tenantId, UUID vehicleId);
    PersistResult persist(Mutation mutation);
}
