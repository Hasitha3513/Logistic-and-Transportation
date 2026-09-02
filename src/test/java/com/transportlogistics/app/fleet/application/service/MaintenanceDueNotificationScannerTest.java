package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceDueNotificationScannerTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-22T00:00:00Z");
    private final Clock clock = Clock.fixed(now.toInstant(), ZoneOffset.UTC);

    @Test void publishesInsideLeadWindowWithStableRepeatedIdentityAndExcludesOutsideWindow() {
        var schedules = mock(MaintenanceScheduleRepository.class);
        var vehicles = mock(VehicleRepository.class);
        var publisher = mock(FleetOperationalNotificationPublisher.class);
        UUID vehicleId = UUID.randomUUID();
        var inside = schedule(UUID.randomUUID(), vehicleId, now.plusHours(23), MaintenanceStatus.SCHEDULED);
        var outside = schedule(UUID.randomUUID(), vehicleId, now.plusHours(25), MaintenanceStatus.SCHEDULED);
        when(schedules.findScheduledStartingBetween(now, now.plusHours(24))).thenReturn(List.of(inside, outside));
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));
        var scanner = new MaintenanceDueNotificationScanner(schedules, vehicles, publisher, clock);

        scanner.scan(); scanner.scan();

        var captor = ArgumentCaptor.forClass(OperationalNotificationEvent.class);
        verify(publisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(OperationalNotificationEvent::eventId)
            .containsExactly(captor.getAllValues().get(0).eventId(), captor.getAllValues().get(0).eventId());
        assertThat(captor.getValue().metadata()).containsEntry("milestone", "DUE_24H");
    }

    @Test void onePublicationFailureDoesNotAbortLaterCandidate() {
        var schedules = mock(MaintenanceScheduleRepository.class);
        var vehicles = mock(VehicleRepository.class);
        var publisher = mock(FleetOperationalNotificationPublisher.class);
        UUID vehicleId = UUID.randomUUID();
        when(schedules.findScheduledStartingBetween(any(), any())).thenReturn(List.of(
            schedule(UUID.randomUUID(), vehicleId, now.plusHours(2), MaintenanceStatus.SCHEDULED),
            schedule(UUID.randomUUID(), vehicleId, now.plusHours(3), MaintenanceStatus.SCHEDULED)));
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));
        doThrow(new IllegalStateException("first failed")).doNothing().when(publisher).publish(any());

        new MaintenanceDueNotificationScanner(schedules, vehicles, publisher, clock).scan();

        verify(publisher, times(2)).publish(any());
    }

    private MaintenanceSchedule schedule(UUID id, UUID vehicleId, OffsetDateTime start, MaintenanceStatus status) {
        return new MaintenanceSchedule(id, vehicleId, "SERVICE", start, start.plusHours(2), status,
            null, null, null, now, now, "a", "a");
    }

    private Vehicle vehicle(UUID id) {
        return new Vehicle(id, "REG-1", null, null, UUID.randomUUID(), UUID.randomUUID(), null, null, null, null,
            "AVAILABLE", null, null, null, true);
    }
}
