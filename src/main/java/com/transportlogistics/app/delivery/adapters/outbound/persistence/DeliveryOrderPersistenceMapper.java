package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import org.springframework.stereotype.Component;

@Component
class DeliveryOrderPersistenceMapper {
    DeliveryOrderEntity toEntity(DeliveryOrder source) {
        var target = new DeliveryOrderEntity();
        target.setId(source.id().value()); target.setDeliveryNumber(source.deliveryNumber().value());
        target.setCustomerId(source.customerId()); target.setOriginLocationId(source.originLocationId());
        target.setDestinationLocationId(source.destinationLocationId()); target.setPriority(source.priority().name());
        target.setServiceType(source.serviceType().name()); target.setWindowStart(source.window().start());
        target.setWindowEnd(source.window().end()); target.setInstructions(source.instructions());
        target.setStatus(source.status().name()); target.setVersion(source.version());
        target.setCreatedAt(source.createdAt()); target.setUpdatedAt(source.updatedAt());
        target.setCreatedBy(source.createdBy()); target.setUpdatedBy(source.updatedBy());
        return target;
    }

    DeliveryOrder toDomain(DeliveryOrderEntity source) {
        return new DeliveryOrder(new DeliveryId(source.getId()), new DeliveryNumber(source.getDeliveryNumber()),
                source.getCustomerId(), source.getOriginLocationId(), source.getDestinationLocationId(),
                DeliveryPriority.valueOf(source.getPriority()), DeliveryServiceType.valueOf(source.getServiceType()),
                new DeliveryWindow(source.getWindowStart(), source.getWindowEnd()), source.getInstructions(),
                DeliveryStatus.valueOf(source.getStatus()), source.getVersion(), source.getCreatedAt(),
                source.getUpdatedAt(), source.getCreatedBy(), source.getUpdatedBy());
    }
}
