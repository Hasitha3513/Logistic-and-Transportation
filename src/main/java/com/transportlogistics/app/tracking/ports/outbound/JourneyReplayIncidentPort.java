package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import java.util.List;

public interface JourneyReplayIncidentPort {
    List<IncidentOverlay> query(ReplayQuery query, CursorState cursor);
}
