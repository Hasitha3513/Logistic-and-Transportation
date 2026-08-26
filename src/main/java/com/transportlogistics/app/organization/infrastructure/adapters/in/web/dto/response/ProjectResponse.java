package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record ProjectResponse(UUID id,
                              String code,
                              String name,
                              UUID departmentId,
                              boolean active) {
}
