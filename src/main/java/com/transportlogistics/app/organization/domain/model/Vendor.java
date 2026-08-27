package com.transportlogistics.app.organization.domain.model;

import java.util.UUID;

public record Vendor(UUID id, String code, String name, String contactPerson, String phone, String email,
                     boolean active) {
}
