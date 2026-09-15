package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopCursorState;

public interface JourneyReplayStopCursorPort {
    String encode(StopCursorState state);
    StopCursorState decode(String opaqueCursor);
}
