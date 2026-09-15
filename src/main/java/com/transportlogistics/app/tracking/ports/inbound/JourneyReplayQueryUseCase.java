package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;

public interface JourneyReplayQueryUseCase {
    ReplayPage points(ReplayQuery query);
    StopPage stops(StopReplayQuery query);
    IncidentPage incidents(ReplayQuery query);
}
