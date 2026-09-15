package com.transportlogistics.app.tracking.domain.journeyreplay;

public final class JourneyReplayException extends RuntimeException {
    private final JourneyReplayError error;

    public JourneyReplayException(JourneyReplayError error) {
        super(error.name());
        this.error = error;
    }

    public JourneyReplayError error() { return error; }
}
