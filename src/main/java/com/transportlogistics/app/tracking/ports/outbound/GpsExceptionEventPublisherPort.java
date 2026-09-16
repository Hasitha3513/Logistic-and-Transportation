package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;

public interface GpsExceptionEventPublisherPort {
    void publishOpened(GpsExceptionEpisode episode);

    void publishResolved(GpsExceptionEpisode episode);
}
