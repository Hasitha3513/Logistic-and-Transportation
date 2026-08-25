package com.transportlogistics.app.freight.order.ports.outbound;

import java.time.OffsetDateTime;

public interface FreightOrderNumberGenerator {
    String next(OffsetDateTime requestedPickupAt);
}
