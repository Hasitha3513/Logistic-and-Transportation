package com.transportlogistics.app.delivery.adapters.inbound.web.mappers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryOrderResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderWebMapper {
    public DeliveryOrderUseCase.CreateCommand toCommand(CreateDeliveryOrderRequest value) {
        return new DeliveryOrderUseCase.CreateCommand(value.customerId(), value.originLocationId(),
                value.destinationLocationId(), value.priority(), value.serviceType(), value.windowStart(),
                value.windowEnd(), value.instructions());
    }
    public DeliveryOrderUseCase.UpdateCommand toCommand(UpdateDeliveryOrderRequest value) {
        return new DeliveryOrderUseCase.UpdateCommand(value.version(), value.customerId(), value.originLocationId(),
                value.destinationLocationId(), value.priority(), value.serviceType(), value.windowStart(),
                value.windowEnd(), value.instructions());
    }
    public DeliveryOrderResponse toResponse(DeliveryOrder value) {
        return new DeliveryOrderResponse(value.id().value(), value.deliveryNumber().value(), value.customerId(),
                value.originLocationId(), value.destinationLocationId(), value.priority(), value.serviceType(),
                value.window().start(), value.window().end(), value.instructions(), value.status(), value.version(),
                value.createdAt(), value.updatedAt(), value.createdBy(), value.updatedBy());
    }
}
