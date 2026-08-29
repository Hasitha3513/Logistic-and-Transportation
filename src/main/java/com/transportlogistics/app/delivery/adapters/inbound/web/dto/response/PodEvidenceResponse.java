package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.PodEvidenceType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PodEvidenceResponse(UUID id, PodEvidenceType type, String barcodeValue, String contentType,
                                  long contentLength, String checksum, String originalFilename,
                                  String captureSource, String createdBy, OffsetDateTime createdAt) {}
