package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelCardResponse(UUID id, UUID providerId, String alias, String maskedIdentifier, String lastFour,
                               int expiryMonth, int expiryYear, String status, String providerSyncStatus,
                               long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
