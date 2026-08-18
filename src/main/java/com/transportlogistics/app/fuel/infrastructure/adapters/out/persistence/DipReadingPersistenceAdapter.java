package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.DipReadingRepository;
import com.transportlogistics.app.fuel.domain.model.DipReading;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class DipReadingPersistenceAdapter implements DipReadingRepository {

    private final DipReadingJpaRepository repository;

    DipReadingPersistenceAdapter(DipReadingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DipReading save(DipReading r) {
        var entity = new DipReadingEntity();
        entity.setId(r.id());
        entity.setTankId(r.tankId());
        entity.setPhysicalQuantityLiters(r.physicalQuantityLiters());
        entity.setBookQuantityAtMeasurement(r.bookQuantityAtMeasurement());
        entity.setVarianceQuantityLiters(r.varianceQuantityLiters());
        entity.setMeasuredAt(r.measuredAt());
        entity.setMeasuredBy(r.measuredBy());
        entity.setNotes(r.notes());
        entity.setCreatedAt(r.createdAt());
        return map(repository.save(entity));
    }

    @Override
    public Optional<DipReading> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<DipReading> findByTankId(UUID tankId) {
        return repository.findByTankIdOrderByMeasuredAtDesc(tankId).stream().map(this::map).toList();
    }

    private DipReading map(DipReadingEntity e) {
        return new DipReading(
                e.getId(),
                e.getTankId(),
                e.getPhysicalQuantityLiters(),
                e.getBookQuantityAtMeasurement(),
                e.getVarianceQuantityLiters(),
                e.getMeasuredAt(),
                e.getMeasuredBy(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }
}
