package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.domain.model.FreightOrderLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
class FreightOrderPersistenceMapper {
    FreightOrderEntity toEntity(FreightOrder order) {
        var entity = new FreightOrderEntity();
        entity.setId(order.id()); entity.setOrderNumber(order.orderNumber()); entity.setCustomerId(order.customerId());
        entity.setOriginLocationId(order.originLocationId()); entity.setDestinationLocationId(order.destinationLocationId());
        entity.setRequestedPickupAt(order.requestedPickupAt()); entity.setRequestedDeliveryAt(order.requestedDeliveryAt());
        entity.setServiceLevel(order.serviceLevel()); entity.setPriority(order.priority());
        entity.setSpecialHandlingInstructions(order.specialHandlingInstructions()); entity.setVersion(order.version());
        entity.setCreatedAt(order.createdAt()); entity.setUpdatedAt(order.updatedAt());
        entity.setCreatedBy(order.createdBy()); entity.setUpdatedBy(order.updatedBy());
        var lines = new ArrayList<FreightOrderLineEntity>();
        for (int index = 0; index < order.lines().size(); index++) {
            FreightOrderLine source = order.lines().get(index);
            var line = new FreightOrderLineEntity(); line.setId(source.id()); line.setDescription(source.description());
            line.setQuantity(source.quantity()); line.setLineOrder(index); lines.add(line);
        }
        entity.replaceLines(lines);
        return entity;
    }

    FreightOrder toDomain(FreightOrderEntity entity) {
        return new FreightOrder(entity.getId(), entity.getOrderNumber(), entity.getCustomerId(), entity.getOriginLocationId(),
                entity.getDestinationLocationId(), entity.getRequestedPickupAt(), entity.getRequestedDeliveryAt(),
                entity.getServiceLevel(), entity.getPriority(), entity.getSpecialHandlingInstructions(),
                entity.getLines().stream().map(line -> new FreightOrderLine(line.getId(), line.getDescription(), line.getQuantity())).toList(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCreatedBy(), entity.getUpdatedBy());
    }
}
