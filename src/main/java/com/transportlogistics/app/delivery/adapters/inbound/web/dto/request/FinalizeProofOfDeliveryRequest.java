package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record FinalizeProofOfDeliveryRequest(@PositiveOrZero long deliveryVersion, @PositiveOrZero long podVersion) {}
