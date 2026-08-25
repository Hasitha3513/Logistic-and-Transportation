package com.transportlogistics.app.freight.order.application;

import com.transportlogistics.app.freight.order.domain.event.FreightOrderCreated;
import com.transportlogistics.app.freight.order.domain.event.FreightOrderUpdated;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.domain.model.FreightOrderLine;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.freight.order.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FreightOrderService implements FreightOrderUseCase {
    private final FreightOrderRepository orders;
    private final FreightOrderNumberGenerator numbers;
    private final FreightCustomerPort customers;
    private final FreightLocationPort locations;
    private final FreightOrderTransaction transactions;
    private final FreightOrderEventPublisher events;
    private final Clock clock;

    public FreightOrderService(FreightOrderRepository orders, FreightOrderNumberGenerator numbers,
                               FreightCustomerPort customers, FreightLocationPort locations,
                               FreightOrderTransaction transactions, FreightOrderEventPublisher events, Clock clock) {
        this.orders = orders; this.numbers = numbers; this.customers = customers; this.locations = locations;
        this.transactions = transactions; this.events = events; this.clock = clock;
    }

    @Override
    public FreightOrder create(CreateCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            OffsetDateTime now = OffsetDateTime.now(clock);
            validateReferences(command.customerId(), command.originLocationId(), command.destinationLocationId());
            FreightOrder order = new FreightOrder(UUID.randomUUID(), numbers.next(command.requestedPickupAt()),
                    command.customerId(), command.originLocationId(), command.destinationLocationId(),
                    command.requestedPickupAt(), command.requestedDeliveryAt(), command.serviceLevel(), command.priority(),
                    command.specialHandlingInstructions(), lines(command.lines()), 0, now, now, actor, actor);
            FreightOrder saved = orders.save(order);
            events.publish(new FreightOrderCreated(saved.id(), saved.orderNumber(), saved.customerId(), actor, now));
            return saved;
        });
    }

    @Override public FreightOrder get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("FREIGHT_ORDER_NOT_FOUND", "Freight order not found: " + id));
    }

    @Override public PageResult<FreightOrder> search(SearchQuery query) { return orders.search(query); }

    @Override
    public FreightOrder update(UUID id, UpdateCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightOrder current = get(id);
            if (command.version() == null) throw new BusinessRuleException("FREIGHT_ORDER_VERSION_REQUIRED", "Version is required for update");
            if (command.version() != current.version()) throw new ConflictException("FREIGHT_ORDER_CONCURRENT_UPDATE", "Freight order was changed by another user");
            UUID customerId = value(command.customerId(), current.customerId());
            UUID originId = value(command.originLocationId(), current.originLocationId());
            UUID destinationId = value(command.destinationLocationId(), current.destinationLocationId());
            validateReferences(customerId, originId, destinationId);
            OffsetDateTime now = OffsetDateTime.now(clock);
            FreightOrder changed = new FreightOrder(current.id(), current.orderNumber(), customerId, originId, destinationId,
                    value(command.requestedPickupAt(), current.requestedPickupAt()),
                    value(command.requestedDeliveryAt(), current.requestedDeliveryAt()),
                    value(command.serviceLevel(), current.serviceLevel()), value(command.priority(), current.priority()),
                    command.specialHandlingInstructions() == null ? current.specialHandlingInstructions() : command.specialHandlingInstructions(),
                    command.lines() == null ? current.lines() : lines(command.lines()), current.version(), current.createdAt(), now,
                    current.createdBy(), actor);
            FreightOrder saved = orders.save(changed);
            events.publish(new FreightOrderUpdated(saved.id(), saved.orderNumber(), saved.version(), actor, now));
            return saved;
        });
    }

    private void validateReferences(UUID customerId, UUID originId, UUID destinationId) {
        var customer = customers.find(customerId).orElseThrow(() -> invalid("FREIGHT_CUSTOMER_NOT_FOUND", "Customer not found: " + customerId));
        if (!customer.active()) throw invalid("FREIGHT_CUSTOMER_INACTIVE", "Inactive customer cannot be used");
        var origin = locations.find(originId).orElseThrow(() -> invalid("FREIGHT_ORIGIN_NOT_FOUND", "Origin location not found: " + originId));
        var destination = locations.find(destinationId).orElseThrow(() -> invalid("FREIGHT_DESTINATION_NOT_FOUND", "Destination location not found: " + destinationId));
        if (!origin.active() || !destination.active()) throw invalid("FREIGHT_LOCATION_INACTIVE", "Origin and destination locations must be active");
    }

    private List<FreightOrderLine> lines(List<LineCommand> values) {
        if (values == null) return List.of();
        return values.stream().map(line -> new FreightOrderLine(line.id() == null ? UUID.randomUUID() : line.id(), line.description(), line.quantity())).toList();
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) throw invalid("FREIGHT_ACTOR_REQUIRED", "An authenticated actor is required");
    }
    private BusinessRuleException invalid(String code, String message) { return new BusinessRuleException(code, message); }
    private <T> T value(T supplied, T current) { return supplied == null ? current : supplied; }
}
