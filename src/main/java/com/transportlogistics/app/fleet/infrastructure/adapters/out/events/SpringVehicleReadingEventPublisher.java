package com.transportlogistics.app.fleet.infrastructure.adapters.out.events;

import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class SpringVehicleReadingEventPublisher implements VehicleReadingEventPublisher {
    private final ApplicationEventPublisher events;

    SpringVehicleReadingEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publishAfterCommit(VehicleReadingRecorded event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }
}
