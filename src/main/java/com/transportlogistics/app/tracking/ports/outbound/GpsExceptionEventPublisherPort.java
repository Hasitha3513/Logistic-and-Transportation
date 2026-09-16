package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;

public interface GpsExceptionEventPublisherPort {
    void publishOpened(GpsExceptionEpisode episode);

    void publishHigh(GpsExceptionEpisode episode);
}
