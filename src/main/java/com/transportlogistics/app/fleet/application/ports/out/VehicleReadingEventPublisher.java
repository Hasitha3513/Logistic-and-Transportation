package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.VehicleReadingRecorded;

public interface VehicleReadingEventPublisher {
    void publishAfterCommit(VehicleReadingRecorded event);
}
