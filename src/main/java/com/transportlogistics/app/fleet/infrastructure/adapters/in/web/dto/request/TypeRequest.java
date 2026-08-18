package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TypeRequest(@NotNull UUID categoryId,
                          @NotBlank String code,
                          @NotBlank String name,
                          String description,
                          Boolean active) {
}
