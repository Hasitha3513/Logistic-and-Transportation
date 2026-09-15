package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredJourneyReplayQueryUseCase implements JourneyReplayQueryUseCase {
    private final JourneyReplayQueryUseCase delegate;
    public SecuredJourneyReplayQueryUseCase(JourneyReplayQueryUseCase delegate) { this.delegate = delegate; }

    @Override @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW')")
    public ReplayPage points(ReplayQuery query) { return delegate.points(query); }

    @Override @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW')")
    public StopPage stops(StopReplayQuery query) { return delegate.stops(query); }

    @Override @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW') and hasAuthority('JOURNEY_REPLAY_INCIDENT_VIEW')")
    public IncidentPage incidents(ReplayQuery query) { return delegate.incidents(query); }
}
