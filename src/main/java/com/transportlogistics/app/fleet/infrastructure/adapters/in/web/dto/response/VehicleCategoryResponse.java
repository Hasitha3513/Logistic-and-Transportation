package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record VehicleCategoryResponse(UUID id,
                                      String code,
                                      String name,
                                      String description,
                                      boolean active) {
}
