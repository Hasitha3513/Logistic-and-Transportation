package com.transportlogistics.app.freight.order.application;

import com.transportlogistics.app.freight.order.domain.event.FreightOrderCreated;
import com.transportlogistics.app.freight.order.domain.event.FreightOrderUpdated;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.freight.order.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FreightOrderServiceTest {
    private final UUID customerId = UUID.randomUUID(), originId = UUID.randomUUID(), destinationId = UUID.randomUUID();
    private FreightOrderRepository orders;
    private FreightCustomerPort customers;
    private FreightLocationPort locations;
    private FreightOrderEventPublisher events;
    private FreightOrderService service;

    @BeforeEach
    void setUp() {
        orders = mock(FreightOrderRepository.class); customers = mock(FreightCustomerPort.class);
        locations = mock(FreightLocationPort.class); events = mock(FreightOrderEventPublisher.class);
        var numbers = mock(FreightOrderNumberGenerator.class); var transaction = mock(FreightOrderTransaction.class);
        when(transaction.execute(any())).thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
        when(numbers.next(any())).thenReturn("FO-2026-000001"); when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customers.find(customerId)).thenReturn(Optional.of(new FreightCustomerPort.CustomerReference(customerId, "C1", "Customer", true)));
        when(locations.find(originId)).thenReturn(Optional.of(new FreightLocationPort.LocationReference(originId, "O1", "Origin", true)));
        when(locations.find(destinationId)).thenReturn(Optional.of(new FreightLocationPort.LocationReference(destinationId, "D1", "Destination", true)));
        service = new FreightOrderService(orders, numbers, customers, locations, transaction, events,
                Clock.fixed(Instant.parse("2026-08-25T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsValidatedOrderAndPublishesEvent() {
        FreightOrder created = service.create(createCommand(), "manager");
        assertEquals("FO-2026-000001", created.orderNumber()); assertEquals(1, created.lines().size());
        assertEquals("manager", created.createdBy()); verify(events).publish(any(FreightOrderCreated.class));
    }

    @Test
    void rejectsMissingOrInactiveReferences() {
        when(customers.find(customerId)).thenReturn(Optional.empty());
        assertCode("FREIGHT_CUSTOMER_NOT_FOUND", () -> service.create(createCommand(), "manager"));
        when(customers.find(customerId)).thenReturn(Optional.of(new FreightCustomerPort.CustomerReference(customerId, "C1", "Customer", false)));
        assertCode("FREIGHT_CUSTOMER_INACTIVE", () -> service.create(createCommand(), "manager"));
    }

    @Test
    void appliesPartialUpdatePreservingBusinessNumberAndAuditOrigin() {
        FreightOrder current = service.create(createCommand(), "creator");
        when(orders.findById(current.id())).thenReturn(Optional.of(current));
        var updated = service.update(current.id(), new FreightOrderUseCase.UpdateCommand(0L, null, null, null,
                null, null, null, "urgent", "Fragile", null), "editor");
        assertEquals(current.orderNumber(), updated.orderNumber()); assertEquals("URGENT", updated.priority());
        assertEquals("creator", updated.createdBy()); assertEquals("editor", updated.updatedBy());
        verify(events).publish(any(FreightOrderUpdated.class));
    }

    @Test
    void rejectsStaleVersionBeforePersistenceOrEvent() {
        FreightOrder current = service.create(createCommand(), "creator"); clearInvocations(orders, events);
        when(orders.findById(current.id())).thenReturn(Optional.of(current));
        assertThrows(ConflictException.class, () -> service.update(current.id(), new FreightOrderUseCase.UpdateCommand(
                2L, null, null, null, null, null, null, null, null, null), "editor"));
        verify(orders, never()).save(any()); verify(events, never()).publish(any());
    }

    private FreightOrderUseCase.CreateCommand createCommand() {
        return new FreightOrderUseCase.CreateCommand(customerId, originId, destinationId,
                OffsetDateTime.parse("2026-09-01T08:00:00Z"), OffsetDateTime.parse("2026-09-02T08:00:00Z"),
                "standard", "normal", null, List.of(new FreightOrderUseCase.LineCommand(null, "Pallets", new BigDecimal("2"))));
    }
    private void assertCode(String code, Runnable operation) {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, operation::run); assertEquals(code, exception.code());
    }
}
