package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record VehicleTypeResponse(UUID id,
                                  UUID categoryId,
                                  String code,
                                  String name,
                                  String description,
                                  boolean active) {
}
