package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalEventRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripLifecyclePolicy;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Transactional
public class TripOperationalEventService implements TripOperationalEventUseCase {

    private static final Set<String> INVALID_RECORDING_STATES = Set.of(
            TripLifecyclePolicy.DRAFT,
            TripLifecyclePolicy.SUBMITTED,
            TripLifecyclePolicy.REJECTED,
            TripLifecyclePolicy.CANCELLED,
            TripLifecyclePolicy.CLOSED
    );

    private final TripRepository tripRepo;
    private final TripOperationalEventRepository eventRepo;
    private final TripHistoryRepository historyRepo;
    private final Clock clock;
    private final ApplicationEventPublisher publisher;


    public TripOperationalEventService(
            TripRepository tripRepo,
            TripOperationalEventRepository eventRepo,
            TripHistoryRepository historyRepo,
            Clock clock,
            ApplicationEventPublisher publisher
    ) {
        this.tripRepo = Objects.requireNonNull(tripRepo, "TripRepository cannot be null");
        this.eventRepo = Objects.requireNonNull(eventRepo, "TripOperationalEventRepository cannot be null");
        this.historyRepo = Objects.requireNonNull(historyRepo, "TripHistoryRepository cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "ApplicationEventPublisher cannot be null");
    }

    @Override
    public TripOperationalEvent recordCheckpoint(UUID tripId, RecordCheckpointCommand command, String actor) {
        var trip = getAndValidateActiveTrip(tripId);
        var now = OffsetDateTime.now(clock);
        var occurredAt = command.occurredAt() != null ? command.occurredAt() : now;
        var actorName = actor != null && !actor.isBlank() ? actor : "system";

        var event = TripOperationalEvent.createCheckpoint(
                UUID.randomUUID(),
                trip.id(),
                command.checkpointType(),
                occurredAt,
                command.locationId(),
                command.locationDescription(),
                command.remarks(),
                actorName,
                now
        );

        var saved = eventRepo.save(event);

        String details = "Checkpoint " + command.checkpointType() + " reached"
                + (command.locationDescription() != null ? " at " + command.locationDescription() : "")
                + (command.remarks() != null ? ": " + command.remarks() : "");
        recordHistory(trip.id(), "CHECKPOINT_RECORDED", trip.status(), details, actorName, occurredAt, now);

        return saved;
    }

    @Override
    public TripOperationalEvent recordDelay(UUID tripId, RecordDelayCommand command, String actor) {
        var trip = getAndValidateActiveTrip(tripId);
        var now = OffsetDateTime.now(clock);
        var occurredAt = command.occurredAt() != null ? command.occurredAt() : now;
        var actorName = actor != null && !actor.isBlank() ? actor : "system";

        var event = TripOperationalEvent.createDelay(
                UUID.randomUUID(),
                trip.id(),
                command.delayMinutes(),
                command.reason(),
                occurredAt,
                command.locationId(),
                command.locationDescription(),
                command.remarks(),
                actorName,
                now
        );

        var saved = eventRepo.save(event);

        // Publish a generic operational notification for the delay event
        var notificationEvent = OperationalNotificationEvent.of(
                "TRIP_DELAY_RECORDED",
                "TRIP",
                trip.id(),
                OperationalNotificationEvent.Severity.INFO,
                "Trip delay recorded",
                "Delay of " + command.delayMinutes() + " mins: " + command.reason(),
                java.util.Map.of()
        );
        publisher.publishEvent(notificationEvent);

        String details = "Delay of " + command.delayMinutes() + " mins recorded: " + command.reason()
                + (command.locationDescription() != null ? " at " + command.locationDescription() : "");
        recordHistory(trip.id(), "DELAY_RECORDED", trip.status(), details, actorName, occurredAt, now);

        return saved;
    }

    @Override
    public TripOperationalEvent recordIncident(UUID tripId, RecordIncidentCommand command, String actor) {
        var trip = getAndValidateActiveTrip(tripId);
        var now = OffsetDateTime.now(clock);
        var occurredAt = command.occurredAt() != null ? command.occurredAt() : now;
        var actorName = actor != null && !actor.isBlank() ? actor : "system";

        var event = TripOperationalEvent.createIncident(
                UUID.randomUUID(),
                trip.id(),
                command.incidentSeverity(),
                command.description(),
                occurredAt,
                command.locationId(),
                command.locationDescription(),
                command.remarks(),
                actorName,
                now
        );

        var saved = eventRepo.save(event);

        String details = "[" + command.incidentSeverity() + "] Incident reported: " + command.description();
        recordHistory(trip.id(), "INCIDENT_RECORDED", trip.status(), details, actorName, occurredAt, now);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripOperationalEvent> getTripEvents(UUID tripId) {
        tripRepo.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));
        return eventRepo.findByTripIdOrderByOccurredAtAsc(tripId);
    }

    @Override
    @Transactional(readOnly = true)
    public TripOperationalEvent getEvent(UUID tripId, UUID eventId) {
        return eventRepo.findById(eventId)
                .filter(e -> e.tripId().equals(tripId))
                .orElseThrow(() -> new NotFoundException("Trip operational event not found: " + eventId));
    }

    private Trip getAndValidateActiveTrip(UUID tripId) {
        var trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));

        if (INVALID_RECORDING_STATES.contains(trip.status())) {
            throw new ConflictException(
                    "CANNOT_RECORD_EVENT",
                    "Cannot record operational event on a " + trip.status() + " trip"
            );
        }

        return trip;
    }

    private void recordHistory(UUID tripId, String action, String status, String details, String actor, OffsetDateTime occurredAt, OffsetDateTime now) {
        try {
            historyRepo.save(new TripHistoryEntry(
                    UUID.randomUUID(),
                    tripId,
                    status,
                    status,
                    action,
                    null,
                    null,
                    null,
                    actor,
                    details,
                    occurredAt != null ? occurredAt : now
            ));
        } catch (Exception ignored) {
            // History append is auxiliary; do not block primary operational event
        }
    }
}
