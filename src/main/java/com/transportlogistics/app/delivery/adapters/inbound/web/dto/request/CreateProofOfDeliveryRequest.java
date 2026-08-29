package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateProofOfDeliveryRequest(@PositiveOrZero long deliveryVersion, OffsetDateTime deviceCapturedAt,
                                           BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters,
                                           String signerName, String signerRelationship) {}
