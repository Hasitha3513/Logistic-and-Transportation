package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.PodStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProofOfDeliveryResponse(UUID id, UUID deliveryOrderId, PodStatus status, OffsetDateTime deviceCapturedAt,
                                      BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters,
                                      String signerName, String signerRelationship, OffsetDateTime acceptedAt,
                                      String acceptedBy, long version, List<PodEvidenceResponse> evidence) {}
