package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredIdleMonitoringQuery implements IdleMonitoringQuery {
    private final IdleMonitoringQuery delegate;
    public SecuredIdleMonitoringQuery(IdleMonitoringQuery delegate){this.delegate=delegate;}
    @Override @PreAuthorize("hasAuthority('IDLE_MONITOR_VIEW')") public Page<State> states(Context c,StateFilter f,String cursor,int limit){return delegate.states(c,f,cursor,limit);}
    @Override @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')") public Page<Episode> episodes(Context c,EpisodeFilter f,String cursor,int limit){return delegate.episodes(c,f,cursor,limit);}
    @Override @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')") public Optional<Episode> episode(Context c,UUID id){return delegate.episode(c,id);}
    @Override @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')") public Page<Evidence> evidence(Context c,UUID id,String cursor,int limit){return delegate.evidence(c,id,cursor,limit);}
}
