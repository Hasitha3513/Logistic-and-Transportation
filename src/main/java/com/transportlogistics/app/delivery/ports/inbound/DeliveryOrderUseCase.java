package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DeliveryOrderUseCase {
    DeliveryOrder create(CreateCommand command, String actor);
    DeliveryOrder get(UUID id);
    PageResult<DeliveryOrder> search(SearchQuery query);
    DeliveryOrder update(UUID id, UpdateCommand command, String actor);
    DeliveryOrder markReady(UUID id, long version, String actor);

    record CreateCommand(UUID customerId, UUID originLocationId, UUID destinationLocationId,
                         DeliveryPriority priority, DeliveryServiceType serviceType,
                         OffsetDateTime windowStart, OffsetDateTime windowEnd, String instructions) {}
    record UpdateCommand(long version, UUID customerId, UUID originLocationId, UUID destinationLocationId,
                         DeliveryPriority priority, DeliveryServiceType serviceType,
                         OffsetDateTime windowStart, OffsetDateTime windowEnd, String instructions) {}
    record SearchQuery(String search, DeliveryStatus status, UUID customerId, OffsetDateTime windowFrom,
                       OffsetDateTime windowTo, int page, int size) {}
    record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
