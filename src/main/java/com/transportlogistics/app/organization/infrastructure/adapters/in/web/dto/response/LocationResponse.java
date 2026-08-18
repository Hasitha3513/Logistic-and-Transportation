package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record LocationResponse(UUID id,
                               String code,
                               String name,
                               String address,
                               Double latitude,
                               Double longitude,
                               boolean active) {
}
