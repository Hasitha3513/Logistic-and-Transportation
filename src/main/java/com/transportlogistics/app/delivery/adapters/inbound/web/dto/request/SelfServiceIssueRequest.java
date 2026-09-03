package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SelfServiceIssueRequest(@NotBlank String category, @NotBlank String description) {}
