package com.transportlogistics.app.integration.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.Min;

public record IntegrationVersionRequest(@Min(0) long version) {}
