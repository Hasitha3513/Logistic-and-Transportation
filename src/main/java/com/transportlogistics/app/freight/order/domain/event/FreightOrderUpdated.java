package com.transportlogistics.app.freight.order.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FreightOrderUpdated(UUID freightOrderId, String orderNumber, long version,
                                  String actor, OffsetDateTime occurredAt) { }
