package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String code,
                              @NotBlank String name,
                              String description,
                              Boolean active) {
}
