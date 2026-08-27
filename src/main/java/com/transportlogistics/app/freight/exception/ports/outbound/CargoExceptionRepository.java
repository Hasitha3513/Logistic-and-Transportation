package com.transportlogistics.app.freight.exception.ports.outbound;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence port for CargoException.
 */
public interface CargoExceptionRepository {

    CargoException save(CargoException exception);

    Optional<CargoException> findById(UUID id);

    List<CargoException> findAll(UUID freightOrderId,
                                 UUID manifestId,
                                 ExceptionType type,
                                 ExceptionStatus status,
                                 int page,
                                 int size);
}
