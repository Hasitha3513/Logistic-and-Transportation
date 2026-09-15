package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ConfirmedStop;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import java.util.List;

public interface JourneyReplayQueryUseCase {
    ReplayPage points(ReplayQuery query);
    List<ConfirmedStop> stops(ReplayQuery query);
    List<IncidentOverlay> incidents(ReplayQuery query);
}
