package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FuelStationRequest(@NotBlank @Size(max = 40) String code,
                                 @NotBlank @Size(max = 160) String name,
                                 @NotNull FuelStationType stationType,
                                 Boolean active,
                                 UUID vendorId,
                                 UUID locationId) {

    public FuelStationUseCase.Command command() {
        return new FuelStationUseCase.Command(code, name, stationType, active, vendorId, locationId);
    }
}
