package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import java.time.OffsetDateTime;

public record PayFineRequest(
        OffsetDateTime paidAt,
        String paymentReference
) {
}
