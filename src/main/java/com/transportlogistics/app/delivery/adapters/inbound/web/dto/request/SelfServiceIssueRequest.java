package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;

public record SelfServiceIssueRequest(@NotBlank String category, @NotBlank String description) {
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown self-service request field: " + field);
    }
}
