package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.CargoExceptionHistoryEntry;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CargoExceptionPersistenceAdapter implements CargoExceptionRepository {

    private final CargoExceptionJpaRepository jpa;
    private final CargoExceptionPersistenceMapper mapper;

    public CargoExceptionPersistenceAdapter(CargoExceptionJpaRepository jpa,
                                            CargoExceptionPersistenceMapper mapper) {
        this.jpa    = jpa;
        this.mapper = mapper;
    }

    @Override
    public CargoException save(CargoException exception) {
        CargoExceptionEntity entity = jpa.findById(exception.getId())
                .orElse(new CargoExceptionEntity());
        mapper.updateEntity(entity, exception);
        CargoExceptionEntity saved = jpa.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CargoException> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CargoException> findAll(UUID freightOrderId,
                                        UUID manifestId,
                                        ExceptionType type,
                                        ExceptionStatus status,
                                        int page,
                                        int size) {
        String typeStr   = type   != null ? type.name()   : null;
        String statusStr = status != null ? status.name() : null;
        PageRequest pageable = PageRequest.of(page, size);
        return jpa.findFiltered(freightOrderId, manifestId, typeStr, statusStr, pageable)
                  .stream()
                  .map(mapper::toDomain)
                  .collect(Collectors.toList());
    }
}
