package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FuelStationPersistenceAdapter implements FuelStationRepository {
    private final FuelStationJpaRepository repository;

    @Override
    public FuelStation save(FuelStation station) {
        var entity = new FuelStationEntity();
        entity.setId(station.id());
        entity.setCode(station.code());
        entity.setName(station.name());
        entity.setStationType(station.stationType());
        entity.setActive(station.active());
        entity.setVendorId(station.vendorId());
        entity.setLocationId(station.locationId());
        return map(repository.save(entity));
    }

    @Override
    public Optional<FuelStation> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<FuelStation> findAll(Boolean active) {
        var values = active == null
                ? repository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                : repository.findByActiveOrderByNameAsc(active);
        return values.stream().map(this::map).toList();
    }

    @Override
    public boolean existsByCode(String code, UUID excludeId) {
        return excludeId == null ? repository.existsByCode(code) : repository.existsByCodeAndIdNot(code, excludeId);
    }

    private FuelStation map(FuelStationEntity entity) {
        return new FuelStation(entity.getId(), entity.getCode(), entity.getName(), entity.getStationType(),
                entity.isActive(), entity.getVendorId(), entity.getLocationId());
    }
}
