package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record SelfServicePreferenceRequest(@NotNull Boolean emailEnabled, @NotNull Boolean smsEnabled, Long version) {
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown self-service request field: " + field);
    }
}
