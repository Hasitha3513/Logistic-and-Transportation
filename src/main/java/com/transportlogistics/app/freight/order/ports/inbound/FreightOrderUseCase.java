package com.transportlogistics.app.freight.order.ports.inbound;

import com.transportlogistics.app.freight.order.domain.model.FreightOrder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FreightOrderUseCase {
    FreightOrder create(CreateCommand command, String actor);
    FreightOrder get(UUID id);
    PageResult<FreightOrder> search(SearchQuery query);
    FreightOrder update(UUID id, UpdateCommand command, String actor);

    record LineCommand(UUID id, String description, BigDecimal quantity) { }

    record CreateCommand(UUID customerId, UUID originLocationId, UUID destinationLocationId,
                         OffsetDateTime requestedPickupAt, OffsetDateTime requestedDeliveryAt,
                         String serviceLevel, String priority, String specialHandlingInstructions,
                         List<LineCommand> lines) { }

    record UpdateCommand(Long version, UUID customerId, UUID originLocationId, UUID destinationLocationId,
                         OffsetDateTime requestedPickupAt, OffsetDateTime requestedDeliveryAt,
                         String serviceLevel, String priority, String specialHandlingInstructions,
                         List<LineCommand> lines) { }

    record SearchQuery(String search, UUID customerId, OffsetDateTime pickupFrom, OffsetDateTime pickupTo,
                       int page, int limit, String sort, String direction) { }

    record PageResult<T>(List<T> content, int page, int limit, long totalElements, int totalPages) { }
}
