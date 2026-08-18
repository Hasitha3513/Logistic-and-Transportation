package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ProjectRequest(@NotBlank String code,
                             @NotBlank String name,
                             UUID departmentId,
                             Boolean active) {
}
