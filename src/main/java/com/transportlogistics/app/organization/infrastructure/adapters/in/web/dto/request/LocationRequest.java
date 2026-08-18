package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LocationRequest(@NotBlank String code,
                              @NotBlank String name,
                              String address,
                              Double latitude,
                              Double longitude,
                              Boolean active) {
}
