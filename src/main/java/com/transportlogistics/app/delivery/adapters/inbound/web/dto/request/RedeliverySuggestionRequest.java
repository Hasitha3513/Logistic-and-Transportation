package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RedeliverySuggestionRequest(
        OffsetDateTime preferredStartTime,
        OffsetDateTime preferredEndTime,
        @Size(max = 500, message = "Customer preference notes must not exceed 500 characters")
        String customerPreferenceNotes
) {
}
