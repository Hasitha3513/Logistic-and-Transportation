package com.transportlogistics.app.freight.order.ports.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreightOrderLookup {
    Optional<OrderReference> find(UUID freightOrderId);

    record OrderReference(UUID id, String orderNumber, List<ExpectedLine> lines) {
        public OrderReference { lines = List.copyOf(lines); }
    }

    record ExpectedLine(UUID id, String description, BigDecimal quantity) { }
}
