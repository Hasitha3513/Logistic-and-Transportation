package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;

import java.time.OffsetDateTime;

public record RedeliverySuggestionResponse(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String slotLabel,
        boolean available,
        String note
) {
    public static RedeliverySuggestionResponse fromDomain(RedeliveryUseCase.RedeliverySuggestion domain) {
        return new RedeliverySuggestionResponse(
                domain.startTime(),
                domain.endTime(),
                domain.slotLabel(),
                domain.available(),
                domain.note()
        );
    }
}
