package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and querying re-delivery schedules.
 */
public interface DeliveryRedeliveryScheduleRepository {

    DeliveryRedeliverySchedule save(DeliveryRedeliverySchedule schedule);

    Optional<DeliveryRedeliverySchedule> findById(UUID id);

    Optional<DeliveryRedeliverySchedule> findCurrentConfirmed(UUID deliveryOrderId);

    List<DeliveryRedeliverySchedule> findByDeliveryOrderId(UUID deliveryOrderId);

    int countActiveOverlapping(UUID tenantId, OffsetDateTime start, OffsetDateTime end, UUID excludeScheduleId);
}
