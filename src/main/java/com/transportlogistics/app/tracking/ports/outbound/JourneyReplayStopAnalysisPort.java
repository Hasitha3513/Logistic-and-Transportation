package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ConfirmedStop;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopAnalysisInput;
import java.util.List;

public interface JourneyReplayStopAnalysisPort {
    List<ConfirmedStop> analyze(StopAnalysisInput input);
}
