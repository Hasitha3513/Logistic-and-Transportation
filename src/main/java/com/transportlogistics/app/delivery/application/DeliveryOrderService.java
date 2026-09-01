package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.domain.events.DeliveryOrderDestinationChangedEvent;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.*;
import java.util.UUID;

public final class DeliveryOrderService implements DeliveryOrderUseCase {
    private final DeliveryOrderRepository orders;
    private final DeliveryNumberGenerator numbers;
    private final DeliveryCustomerLookupPort customers;
    private final DeliveryLocationLookupPort locations;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final DeliveryOrderEventPublisherPort eventPublisher;
    private final Clock clock;

    public DeliveryOrderService(DeliveryOrderRepository orders, DeliveryNumberGenerator numbers,
                                DeliveryCustomerLookupPort customers, DeliveryLocationLookupPort locations,
                                DeliveryTenantContextPort tenantContext, DeliveryOrderTransaction transactions,
                                DeliveryOrderEventPublisherPort eventPublisher, Clock clock) {
        this.orders = orders; this.numbers = numbers; this.customers = customers; this.locations = locations;
        this.tenantContext = tenantContext; this.transactions = transactions; this.eventPublisher = eventPublisher; this.clock = clock;
    }

    @Override
    public DeliveryOrder create(CreateCommand command, String actor) {
        return transactions.execute(() -> {
            var tenant = requiredTenant();
            validateReferences(command.customerId(), command.originLocationId(), command.destinationLocationId());
            OffsetDateTime now = OffsetDateTime.now(clock);
            Year year = Year.from(ZonedDateTime.now(clock).withZoneSameInstant(zone(tenant.timeZone())));
            DeliveryNumber number = allocateUniqueNumber(tenant.tenantId(), year);
            DeliveryOrder order = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()),
                    number, command.customerId(), command.originLocationId(),
                    command.destinationLocationId(), defaultPriority(command.priority()), defaultService(command.serviceType()),
                    new DeliveryWindow(command.windowStart(), command.windowEnd()), command.instructions(), now, actor);
            return orders.save(order);
        });
    }

    @Override public DeliveryOrder get(UUID id) {
        requiredTenant();
        return orders.findById(id).orElseThrow(() -> missing(id));
    }

    @Override public PageResult<DeliveryOrder> search(SearchQuery query) {
        requiredTenant();
        return orders.search(query);
    }

    @Override
    public DeliveryOrder update(UUID id, UpdateCommand command, String actor) {
        return transactions.execute(() -> {
            DeliveryOrder current = get(id);
            requireVersion(command.version(), current.version());
            validateReferences(command.customerId(), command.originLocationId(), command.destinationLocationId());
            DeliveryOrder saved = orders.save(current.updateRequirements(command.customerId(), command.originLocationId(),
                    command.destinationLocationId(), command.priority(), command.serviceType(),
                    new DeliveryWindow(command.windowStart(), command.windowEnd()), command.instructions(),
                    OffsetDateTime.now(clock), actor));
            if (!current.destinationLocationId().equals(saved.destinationLocationId())) {
                eventPublisher.publishEvent(new DeliveryOrderDestinationChangedEvent(requiredTenant().tenantId(), id,
                        current.destinationLocationId(), saved.destinationLocationId(), OffsetDateTime.now(clock), actor));
            }
            return saved;
        });
    }

    @Override
    public DeliveryOrder markReady(UUID id, long version, String actor) {
        return transactions.execute(() -> {
            DeliveryOrder current = get(id);
            requireVersion(version, current.version());
            validateReferences(current.customerId(), current.originLocationId(), current.destinationLocationId());
            return orders.save(current.markReadyForAssignment(OffsetDateTime.now(clock), actor));
        });
    }

    private DeliveryTenantContextPort.TenantContext requiredTenant() {
        return tenantContext.currentTenant().orElseThrow(() ->
                new BusinessRuleException("TENANT_CONTEXT_REQUIRED", "An active Tenant context is required"));
    }
    private void validateReferences(UUID customerId, UUID originId, UUID destinationId) {
        if (customerId == null || originId == null || destinationId == null)
            throw invalid("DELIVERY_REFERENCE_REQUIRED", "Customer, origin and destination are required");
        if (originId.equals(destinationId)) throw invalid("INVALID_DELIVERY_LOCATIONS", "Origin and destination must be different");
        var customer = customers.findCustomer(customerId).orElseThrow(() -> invalid("DELIVERY_REFERENCE_NOT_ELIGIBLE", "Customer is unavailable"));
        if (!customer.active()) throw invalid("DELIVERY_REFERENCE_NOT_ELIGIBLE", "Customer is unavailable");
        var origin = locations.findLocation(originId).orElseThrow(() -> invalid("DELIVERY_REFERENCE_NOT_ELIGIBLE", "Origin location is unavailable"));
        var destination = locations.findLocation(destinationId).orElseThrow(() -> invalid("DELIVERY_REFERENCE_NOT_ELIGIBLE", "Destination location is unavailable"));
        if (!origin.active() || !destination.active()) throw invalid("DELIVERY_REFERENCE_NOT_ELIGIBLE", "Delivery locations are unavailable");
    }
    private void requireVersion(long supplied, long current) {
        if (supplied != current) throw new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order was changed by another user");
    }
    private DeliveryPriority defaultPriority(DeliveryPriority value) { return value == null ? DeliveryPriority.NORMAL : value; }
    private DeliveryNumber allocateUniqueNumber(UUID tenantId, Year year) {
        for (int attempt = 0; attempt < 3; attempt++) {
            DeliveryNumber candidate = numbers.next(tenantId, year);
            if (orders.findByDeliveryNumber(candidate.value()).isEmpty()) return candidate;
        }
        throw invalid("DELIVERY_NUMBER_ALLOCATION_FAILED", "A Delivery number could not be allocated");
    }
    private DeliveryServiceType defaultService(DeliveryServiceType value) { return value == null ? DeliveryServiceType.STANDARD : value; }
    private ZoneId zone(String value) {
        try { return ZoneId.of(value); }
        catch (DateTimeException error) { throw invalid("TENANT_TIME_ZONE_INVALID", "Tenant time zone is unavailable"); }
    }
    private NotFoundException missing(UUID id) { return new NotFoundException("DELIVERY_NOT_FOUND", "Delivery Order not found: " + id); }
    private BusinessRuleException invalid(String code, String message) { return new BusinessRuleException(code, message); }
}
