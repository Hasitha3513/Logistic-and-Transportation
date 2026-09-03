package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SelfServicePreferenceRequest(@NotNull Boolean emailEnabled, @NotNull Boolean smsEnabled, Long version) {}
