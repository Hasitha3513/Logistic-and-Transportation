package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(@NotBlank String code,
                                @NotBlank String name,
                                String description,
                                Boolean active) {
}
