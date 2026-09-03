package com.transportlogistics.app.fleet.infrastructure.adapters.out.events;

import com.transportlogistics.app.fleet.VehicleMeterResetRecorded;
import com.transportlogistics.app.fleet.VehicleReadingCorrected;
import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringVehicleReadingEventPublisher implements VehicleReadingEventPublisher {
    private final AfterCommitEventPublisher publisher;

    SpringVehicleReadingEventPublisher(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAfterCommit(VehicleReadingRecorded event) {
        publisher.publish(event);
    }

    @Override
    public void publishAfterCommit(VehicleReadingCorrected event) {
        publisher.publish(event);
    }

    @Override
    public void publishAfterCommit(VehicleMeterResetRecorded event) {
        publisher.publish(event);
    }
}
