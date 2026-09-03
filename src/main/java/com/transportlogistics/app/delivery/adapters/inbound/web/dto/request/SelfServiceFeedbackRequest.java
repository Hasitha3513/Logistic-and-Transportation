package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SelfServiceFeedbackRequest(@Min(1) @Max(5) int rating, String comment) {}
