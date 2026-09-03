package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionCase;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionType;
import com.transportlogistics.app.delivery.domain.model.DeliveryId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryExceptionRepository {
    DeliveryExceptionCase save(DeliveryExceptionCase exceptionCase);
    Optional<DeliveryExceptionCase> findById(UUID id);
    List<DeliveryExceptionCase> findByDeliveryOrderId(DeliveryId deliveryOrderId);
    boolean existsActiveByDeliveryOrderIdAndType(DeliveryId deliveryOrderId, DeliveryExceptionType exceptionType);
    boolean hasActiveBlockingExceptions(DeliveryId deliveryOrderId);
}
