package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import java.util.List;

public interface JourneyReplayQueryUseCase {
    ReplayPage points(ReplayQuery query);
    StopPage stops(StopReplayQuery query);
    List<IncidentOverlay> incidents(ReplayQuery query);
}
