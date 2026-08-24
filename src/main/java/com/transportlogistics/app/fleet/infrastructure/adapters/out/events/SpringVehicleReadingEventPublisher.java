package com.transportlogistics.app.fleet.infrastructure.adapters.out.events;

import com.transportlogistics.app.fleet.VehicleMeterResetRecorded;
import com.transportlogistics.app.fleet.VehicleReadingCorrected;
import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringVehicleReadingEventPublisher implements VehicleReadingEventPublisher {
    private final ApplicationEventPublisher publisher;

    SpringVehicleReadingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAfterCommit(VehicleReadingRecorded event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishAfterCommit(VehicleReadingCorrected event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishAfterCommit(VehicleMeterResetRecorded event) {
        publisher.publishEvent(event);
    }
}
