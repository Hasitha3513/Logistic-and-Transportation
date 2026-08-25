package com.transportlogistics.app.freight.order.ports.outbound;

import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;

import java.util.Optional;
import java.util.UUID;

public interface FreightOrderRepository {
    FreightOrder save(FreightOrder order);
    Optional<FreightOrder> findById(UUID id);
    FreightOrderUseCase.PageResult<FreightOrder> search(FreightOrderUseCase.SearchQuery query);
}
