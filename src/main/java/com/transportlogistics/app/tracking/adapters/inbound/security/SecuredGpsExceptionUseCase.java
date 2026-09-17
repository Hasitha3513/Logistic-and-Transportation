package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredGpsExceptionUseCase implements GpsExceptionUseCase {
    private final GpsExceptionUseCase delegate;
    public SecuredGpsExceptionUseCase(GpsExceptionUseCase delegate){this.delegate=delegate;}
    @Override @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')") public Page<GpsExceptionEpisode> episodes(Context c,Filter f,String cursor,int limit){return delegate.episodes(c,f,cursor,limit);}
    @Override @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')") public Optional<GpsExceptionEpisode> episode(Context c,UUID id){return delegate.episode(c,id);}
    @Override @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')") public Page<GpsExceptionEvidence> evidence(Context c,UUID id,String cursor,int limit){return delegate.evidence(c,id,cursor,limit);}
    @Override @PreAuthorize("hasAuthority('GPS_EXCEPTION_REVIEW')") public Acknowledgement acknowledge(Context c,UUID id,long version,String reason,String key){return delegate.acknowledge(c,id,version,reason,key);}
}
