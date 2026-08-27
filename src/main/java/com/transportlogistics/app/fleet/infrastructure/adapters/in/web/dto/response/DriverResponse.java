package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record DriverResponse(UUID id,
                             String employeeNumber,
                             String firstName,
                             String lastName,
                             String phone,
                             String email,
                             String status,
                             boolean active) {
}
