package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DriverRequest(@NotBlank String employeeNumber,
                            @NotBlank String firstName,
                            @NotBlank String lastName,
                            String phone,
                            @Email String email,
                            String status,
                            Boolean active) {
}
