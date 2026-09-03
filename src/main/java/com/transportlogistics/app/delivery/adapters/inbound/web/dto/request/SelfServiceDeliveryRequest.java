package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.time.OffsetDateTime;

public record SelfServiceDeliveryRequest(OffsetDateTime preferredStartAt, OffsetDateTime preferredEndAt, String notes) {
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown self-service request field: " + field);
    }
}
