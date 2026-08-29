package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record DeliveryVersionRequest(@PositiveOrZero long version) {}
