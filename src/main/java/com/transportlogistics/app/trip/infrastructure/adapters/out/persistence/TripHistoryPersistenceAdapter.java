package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TripHistoryPersistenceAdapter implements TripHistoryRepository {
    private final TripHistoryJpaRepository repository;

    @Override
    public TripHistoryEntry save(TripHistoryEntry entry) {
        var entity = new TripHistoryEntity();
        entity.setId(entry.id());
        entity.setTripId(entry.tripId());
        entity.setFromStatus(entry.fromStatus());
        entity.setToStatus(entry.toStatus());
        entity.setAction(entry.action());
        entity.setVehicleId(entry.vehicleId());
        entity.setDriverId(entry.driverId());
        entity.setLicenseClass(entry.licenseClass());
        entity.setActor(entry.actor());
        entity.setDetails(entry.details());
        entity.setOccurredAt(entry.occurredAt());
        return map(repository.save(entity));
    }

    @Override
    public List<TripHistoryEntry> findByTripId(UUID tripId) {
        return repository.findByTripIdOrderByOccurredAtAsc(tripId).stream().map(this::map).toList();
    }

    @Override
    public java.util.Optional<String> findCurrentDriverLicenseClass(UUID tripId, UUID driverId) {
        return repository.findFirstByTripIdAndDriverIdAndLicenseClassIsNotNullAndActionInOrderByOccurredAtDesc(
                        tripId, driverId, List.of("DRIVER_ASSIGNED", "DRIVER_REASSIGNED"))
                .map(TripHistoryEntity::getLicenseClass);
    }

    private TripHistoryEntry map(TripHistoryEntity entity) {
        return new TripHistoryEntry(entity.getId(), entity.getTripId(), entity.getFromStatus(),
                entity.getToStatus(), entity.getAction(), entity.getVehicleId(), entity.getDriverId(),
                entity.getLicenseClass(), entity.getActor(), entity.getDetails(), entity.getOccurredAt());
    }
}
