package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.VehicleMeterResetRecorded;
import com.transportlogistics.app.fleet.VehicleReadingCorrected;
import com.transportlogistics.app.fleet.VehicleReadingRecorded;

public interface VehicleReadingEventPublisher {
    void publishAfterCommit(VehicleReadingRecorded event);
    void publishAfterCommit(VehicleReadingCorrected event);
    void publishAfterCommit(VehicleMeterResetRecorded event);
}