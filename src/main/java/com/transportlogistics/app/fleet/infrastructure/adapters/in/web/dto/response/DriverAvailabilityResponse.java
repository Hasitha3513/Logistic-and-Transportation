package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.DriverAvailability;

import java.util.List;

public record DriverAvailabilityResponse(boolean available, List<ReasonDto> reasons) {

    public record ReasonDto(String code, String message) {
    }

    public static DriverAvailabilityResponse from(DriverAvailability domain) {
        if (domain == null) return null;
        var reasons = domain.reasons().stream()
                .map(r -> new ReasonDto(r.code().name(), r.message()))
                .toList();
        return new DriverAvailabilityResponse(domain.available(), reasons);
    }
}
