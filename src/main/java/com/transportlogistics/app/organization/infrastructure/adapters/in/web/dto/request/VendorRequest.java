package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VendorRequest(@NotBlank String code,
                            @NotBlank String name,
                            String contactPerson,
                            String phone,
                            @Email String email,
                            Boolean active) {
}
