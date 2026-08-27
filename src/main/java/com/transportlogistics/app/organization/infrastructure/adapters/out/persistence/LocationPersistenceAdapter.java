package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.organization.application.ports.out.LocationRepository;
import com.transportlogistics.app.organization.domain.model.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LocationPersistenceAdapter implements LocationRepository {
    private final LocationJpaRepository repo;

    public Location save(Location v) {
        var e = new LocationEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setAddress(v.address());
        e.setLatitude(v.latitude());
        e.setLongitude(v.longitude());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Location> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<Location> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Location map(LocationEntity e) {
        return new Location(e.getId(), e.getCode(), e.getName(), e.getAddress(), e.getLatitude(), e.getLongitude(), e.isActive());
    }
}
