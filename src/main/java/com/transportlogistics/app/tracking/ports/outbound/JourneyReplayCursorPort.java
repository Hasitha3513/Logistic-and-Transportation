package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;

public interface JourneyReplayCursorPort {
    String encode(CursorState state);
    CursorState decode(String opaqueCursor);
}
