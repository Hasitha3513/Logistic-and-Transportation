package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;

import java.util.List;

public record VehicleAvailabilityResponse(boolean available, List<ReasonDto> reasons) {

    public record ReasonDto(String code, String message) {
    }

    public static VehicleAvailabilityResponse from(VehicleAvailability domain) {
        if (domain == null) return null;
        var reasons = domain.reasons().stream()
                .map(r -> new ReasonDto(r.code().name(), r.message()))
                .toList();
        return new VehicleAvailabilityResponse(domain.available(), reasons);
    }
}
