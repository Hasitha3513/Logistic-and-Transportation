package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryOrderRepository {
    DeliveryOrder save(DeliveryOrder order);
    Optional<DeliveryOrder> findById(UUID id);
    Optional<DeliveryOrder> findByIdForUpdate(UUID id);
    Optional<DeliveryOrder> findByDeliveryNumber(String deliveryNumber);
    DeliveryOrderUseCase.PageResult<DeliveryOrder> search(DeliveryOrderUseCase.SearchQuery query);
}
