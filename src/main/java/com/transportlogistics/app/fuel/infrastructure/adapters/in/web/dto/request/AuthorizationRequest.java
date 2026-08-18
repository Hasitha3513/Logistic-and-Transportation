package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Size;

public record AuthorizationRequest(@Size(max = 1000) String comment) {
}
