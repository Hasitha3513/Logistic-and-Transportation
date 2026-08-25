package com.transportlogistics.app.freight.order.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FreightOrderCreated(UUID freightOrderId, String orderNumber, UUID customerId,
                                  String actor, OffsetDateTime occurredAt) { }
