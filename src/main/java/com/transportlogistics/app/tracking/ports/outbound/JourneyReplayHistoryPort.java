package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import java.util.UUID;

public interface JourneyReplayHistoryPort {
    ReplayPage query(ReplayQuery query, UUID resolvedVehicleId, CursorState cursor);
}
