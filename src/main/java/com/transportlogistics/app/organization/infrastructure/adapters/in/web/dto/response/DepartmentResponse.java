package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record DepartmentResponse(UUID id,
                                 String code,
                                 String name,
                                 String description,
                                 boolean active) {
}
