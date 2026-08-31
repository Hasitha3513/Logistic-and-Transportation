package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryRiderDutyStatusRequest(
        @NotNull UUID shiftId,
        @NotBlank String action // "START_DUTY", "COMPLETE_DUTY", "CANCEL_SHIFT"
) {
}
