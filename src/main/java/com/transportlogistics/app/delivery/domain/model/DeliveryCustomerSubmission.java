package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryCustomerSubmission(UUID id, UUID tenantId, UUID deliveryOrderId, UUID customerId,
        UUID accessId, CustomerSubmissionType type, String category, String description, Integer rating,
        OffsetDateTime preferredStartAt, OffsetDateTime preferredEndAt, String status, String idempotencyKey,
        String requestHash, OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {}
