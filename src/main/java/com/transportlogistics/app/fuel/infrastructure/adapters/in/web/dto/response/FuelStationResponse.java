package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.FuelStationType;

import java.util.UUID;

public record FuelStationResponse(UUID id,
                                  String code,
                                  String name,
                                  FuelStationType stationType,
                                  boolean active,
                                  UUID vendorId,
                                  UUID locationId) {
}
