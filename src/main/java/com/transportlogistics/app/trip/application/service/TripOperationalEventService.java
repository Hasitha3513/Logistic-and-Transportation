package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalEventRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalNotificationPublisher;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripLifecyclePolicy;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Transactional(noRollbackFor = {ConflictException.class, NotFoundException.class})
public class TripOperationalEventService implements TripOperationalEventUseCase, TripOperationalEventRecorder {
    private static final Logger log = LoggerFactory.getLogger(TripOperationalEventService.class);

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
    private final TripOperationalNotificationPublisher publisher;


    public TripOperationalEventService(
            TripRepository tripRepo,
            TripOperationalEventRepository eventRepo,
            TripHistoryRepository historyRepo,
            Clock clock,
            TripOperationalNotificationPublisher publisher
    ) {
        this.tripRepo = Objects.requireNonNull(tripRepo, "TripRepository cannot be null");
        this.eventRepo = Objects.requireNonNull(eventRepo, "TripOperationalEventRepository cannot be null");
        this.historyRepo = Objects.requireNonNull(historyRepo, "TripHistoryRepository cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "TripOperationalNotificationPublisher cannot be null");
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
    public Result recordCheckpoint(CheckpointCommand command) {
        TripOperationalEvent saved = recordCheckpoint(command.tripId(), new RecordCheckpointCommand(
                com.transportlogistics.app.trip.domain.model.TripCheckpointType.valueOf(command.checkpointType().name()),
                command.occurredAt(), command.locationId(), command.locationDescription(), command.remarks()),
                command.actor());
        return new Result(saved.id(), saved.tripId(), saved.occurredAt());
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

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("tripId", trip.id().toString());
        metadata.put("tripNumber", trip.tripNumber());
        metadata.put("delayMinutes", Integer.toString(saved.delayMinutes()));
        metadata.put("reason", saved.reason());
        if (saved.locationDescription() != null && !saved.locationDescription().isBlank()) {
            metadata.put("locationDescription", saved.locationDescription());
        }
        publishSafely(new OperationalNotificationEvent(saved.id(), "TRIP_DELAY_RECORDED", "TRIP", trip.id(),
                OperationalNotificationEvent.Severity.WARNING, "Trip delay recorded",
                "Delay of " + saved.delayMinutes() + " mins: " + saved.reason(), saved.occurredAt(), metadata));

        String details = "Delay of " + command.delayMinutes() + " mins recorded: " + command.reason()
                + (command.locationDescription() != null ? " at " + command.locationDescription() : "");
        recordHistory(trip.id(), "DELAY_RECORDED", trip.status(), details, actorName, occurredAt, now);

        return saved;
    }

    @Override
    public Result recordDelay(DelayCommand command) {
        TripOperationalEvent saved = recordDelay(command.tripId(), new RecordDelayCommand(
                command.delayMinutes(), command.reason(), command.occurredAt(), command.locationId(),
                command.locationDescription(), command.remarks()), command.actor());
        return new Result(saved.id(), saved.tripId(), saved.occurredAt());
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

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("tripId", trip.id().toString());
        metadata.put("tripNumber", trip.tripNumber());
        metadata.put("incidentSeverity", saved.incidentSeverity().name());
        metadata.put("description", saved.reason());
        if (saved.locationDescription() != null && !saved.locationDescription().isBlank()) {
            metadata.put("locationDescription", saved.locationDescription());
        }
        publishSafely(new OperationalNotificationEvent(saved.id(), "TRIP_INCIDENT_RECORDED", "TRIP", trip.id(),
                incidentSeverity(saved.incidentSeverity()), "Trip incident recorded", saved.reason(),
                saved.occurredAt(), metadata));

        String details = "[" + command.incidentSeverity() + "] Incident reported: " + command.description();
        recordHistory(trip.id(), "INCIDENT_RECORDED", trip.status(), details, actorName, occurredAt, now);

        return saved;
    }

    @Override
    public Result recordIncident(IncidentCommand command) {
        TripOperationalEvent saved = recordIncident(command.tripId(), new RecordIncidentCommand(
                com.transportlogistics.app.trip.domain.model.TripIncidentSeverity.valueOf(
                        command.incidentSeverity().name()),
                command.description(), command.occurredAt(), command.locationId(), command.locationDescription(),
                command.remarks()), command.actor());
        return new Result(saved.id(), saved.tripId(), saved.occurredAt());
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

    private OperationalNotificationEvent.Severity incidentSeverity(
            com.transportlogistics.app.trip.domain.model.TripIncidentSeverity severity) {
        return switch (severity) {
            case LOW -> OperationalNotificationEvent.Severity.INFO;
            case MEDIUM, HIGH -> OperationalNotificationEvent.Severity.WARNING;
            case CRITICAL -> OperationalNotificationEvent.Severity.CRITICAL;
        };
    }

    private void publishSafely(OperationalNotificationEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException exception) {
            log.error("Operational notification publication failed for trip event {}", event.eventId(), exception);
        }
    }
}
