package com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record VendorResponse(UUID id,
                             String code,
                             String name,
                             String contactPerson,
                             String phone,
                             String email,
                             boolean active) {
}
