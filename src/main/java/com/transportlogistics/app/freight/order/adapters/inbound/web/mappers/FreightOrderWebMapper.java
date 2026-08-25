package com.transportlogistics.app.freight.order.adapters.inbound.web.mappers;

import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response.*;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FreightOrderWebMapper {
    default FreightOrderUseCase.CreateCommand toCommand(CreateFreightOrderRequest request) {
        return new FreightOrderUseCase.CreateCommand(request.customerId(), request.originLocationId(), request.destinationLocationId(),
                request.requestedPickupAt(), request.requestedDeliveryAt(), request.serviceLevel(), request.priority(),
                request.specialHandlingInstructions(), request.lines().stream().map(this::toLineCommand).toList());
    }

    default FreightOrderUseCase.UpdateCommand toCommand(UpdateFreightOrderRequest request) {
        return new FreightOrderUseCase.UpdateCommand(request.version(), request.customerId(), request.originLocationId(),
                request.destinationLocationId(), request.requestedPickupAt(), request.requestedDeliveryAt(), request.serviceLevel(),
                request.priority(), request.specialHandlingInstructions(), request.lines() == null ? null : request.lines().stream().map(this::toLineCommand).toList());
    }

    default FreightOrderUseCase.LineCommand toLineCommand(FreightOrderLineRequest request) {
        return new FreightOrderUseCase.LineCommand(request.id(), request.description(), request.quantity());
    }

    default FreightOrderResponse toResponse(FreightOrder order) {
        return new FreightOrderResponse(order.id(), order.orderNumber(), order.customerId(), order.originLocationId(),
                order.destinationLocationId(), order.requestedPickupAt(), order.requestedDeliveryAt(), order.serviceLevel(),
                order.priority(), order.specialHandlingInstructions(), order.lines().stream()
                .map(line -> new FreightOrderLineResponse(line.id(), line.description(), line.quantity())).toList(),
                order.version(), order.createdAt(), order.updatedAt(), order.createdBy(), order.updatedBy());
    }
}
