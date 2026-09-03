package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import java.util.List;
import java.util.UUID;

public record DeliveryFailureHistoryResponse(
        UUID deliveryId,
        int totalAttempts,
        List<DeliveryAttemptResponse> attempts,
        List<DeliveryEscalationResponse> escalations
) {}
