package com.transportlogistics.app.freight.order.application;

import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderRepository;

import java.util.Optional;
import java.util.UUID;

public final class FreightOrderLookupService implements FreightOrderLookup {
    private final FreightOrderRepository orders;

    public FreightOrderLookupService(FreightOrderRepository orders) { this.orders = orders; }

    @Override
    public Optional<OrderReference> find(UUID freightOrderId) {
        return orders.findById(freightOrderId).map(order -> new OrderReference(order.id(), order.orderNumber(),
                order.lines().stream().map(line -> new ExpectedLine(line.id(), line.description(), line.quantity())).toList()));
    }
}
