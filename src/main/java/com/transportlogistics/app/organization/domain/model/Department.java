package com.transportlogistics.app.organization.domain.model;

import java.util.UUID;

public record Department(UUID id, String code, String name, String description, boolean active) {
}
